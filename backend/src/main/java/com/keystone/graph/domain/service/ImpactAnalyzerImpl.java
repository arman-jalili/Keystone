// Canonical Reference: .pi/architecture/modules/dependency-graph.md#impact-analyzer
// Implements: ImpactAnalyzer
// Issue: #76
package com.keystone.graph.domain.service;

import com.keystone.analysis.domain.event.BreakingChangeReportedEvent;
import com.keystone.graph.domain.event.DownstreamImpactComputedEvent;
import com.keystone.graph.domain.model.ApiDependency;
import com.keystone.graph.domain.model.ImpactAnalysisResult;
import com.keystone.graph.domain.model.ImpactAnalysisResult.AffectedService;
import com.keystone.graph.domain.model.ImpactAnalysisResult.ImpactSeverity;
import com.keystone.graph.infrastructure.event.GraphEventPublisher;
import com.keystone.graph.infrastructure.repository.GraphRepository;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

/**
 * Implementation of {@link ImpactAnalyzer} using BFS traversal.
 *
 * <p>Canonical Reference: .pi/architecture/modules/dependency-graph.md#impact-analyzer
 * <p>Implements: ImpactAnalyzer
 * <p>Issue: #76
 *
 * <p>Performs breadth-first traversal of the dependency graph to find all
 * downstream services affected by a breaking change. Handles circular
 * dependencies via a visited set. Classifies first-level consumers as
 * DIRECT impact and deeper transitive consumers as TRANSITIVE.
 *
 * <p>Also listens for {@link BreakingChangeReportedEvent} from the Breaking
 * Change Analysis context to automatically trigger impact analysis when
 * a breaking change is detected.
 */
@Service
public class ImpactAnalyzerImpl implements ImpactAnalyzer {

    private static final Logger log = LoggerFactory.getLogger(ImpactAnalyzerImpl.class);

    private final GraphRepository graphRepository;
    private final GraphEventPublisher eventPublisher;

    public ImpactAnalyzerImpl(GraphRepository graphRepository, GraphEventPublisher eventPublisher) {
        this.graphRepository = graphRepository;
        this.eventPublisher = eventPublisher;
    }

    @Override
    public ImpactAnalysisResult computeImpact(String specPath, UUID reportId) {
        log.info("Computing impact for spec '{}' (reportId: {})", specPath, reportId);

        List<com.keystone.graph.domain.model.Service> producers = graphRepository.findProducersBySpecPath(specPath);
        if (producers.isEmpty()) {
            log.info("No producers found for spec '{}'", specPath);
            return new ImpactAnalysisResult(reportId, List.of());
        }

        return computeImpactFromProducers(specPath, producers, reportId);
    }

    @Override
    public ImpactAnalysisResult computeImpactFromProducer(String specPath, UUID producerId, UUID reportId) {
        log.info("Computing impact for spec '{}' from producer '{}' (reportId: {})", specPath, producerId, reportId);

        com.keystone.graph.domain.model.Service producer =
                graphRepository.findServiceById(producerId).orElse(null);
        if (producer == null) {
            log.warn("Producer service '{}' not found for spec '{}'", producerId, specPath);
            return new ImpactAnalysisResult(reportId, List.of());
        }

        return computeImpactFromProducers(specPath, List.of(producer), reportId);
    }

    /**
     * Core BFS traversal logic. Starts from the given producers and follows
     * consumer edges to find all affected downstream services.
     */
    private ImpactAnalysisResult computeImpactFromProducers(
            String specPath, List<com.keystone.graph.domain.model.Service> producers, UUID reportId) {

        Set<UUID> visited = new HashSet<>();
        Queue<BfsNode> queue = new ArrayDeque<>();
        List<AffectedService> affected = new ArrayList<>();

        // Seed the BFS: start with each producer
        for (com.keystone.graph.domain.model.Service producer : producers) {
            visited.add(producer.getId());
            queue.add(new BfsNode(producer, producer.getName(), 0));
        }

        while (!queue.isEmpty()) {
            BfsNode current = queue.poll();
            com.keystone.graph.domain.model.Service currentService = current.service;
            int depth = current.depth;

            // Find all consumers of this service
            List<ApiDependency> consumers = graphRepository.findConsumers(currentService.getId());

            for (ApiDependency dep : consumers) {
                UUID consumerId = dep.getConsumerId();
                if (consumerId == null) {
                    continue; // skip producer-only edges
                }

                if (visited.contains(consumerId)) {
                    continue; // prevent cycles
                }

                visited.add(consumerId);

                // Find the consumer service name
                com.keystone.graph.domain.model.Service consumerService =
                        graphRepository.findServiceById(consumerId).orElse(null);
                if (consumerService == null) {
                    continue;
                }

                // Determine impact severity: first-level consumers are DIRECT
                ImpactSeverity severity = (depth == 0) ? ImpactSeverity.DIRECT : ImpactSeverity.TRANSITIVE;

                // Build the dependency path
                String path = current.path + " → " + consumerService.getName();

                affected.add(new AffectedService(consumerService.getId(), consumerService.getName(), severity, path));

                // Enqueue for further traversal
                queue.add(new BfsNode(consumerService, path, depth + 1));
            }
        }

        ImpactAnalysisResult result = new ImpactAnalysisResult(reportId, affected);

        // Publish event for downstream consumers
        DownstreamImpactComputedEvent event = new DownstreamImpactComputedEvent(
                UUID.randomUUID(),
                reportId,
                affected,
                affected.size(),
                Instant.now(),
                reportId.toString() + "-" + specPath);
        eventPublisher.downstreamImpactComputed(event);

        log.info("Impact analysis complete: {} affected services found", affected.size());
        return result;
    }

    @Override
    public boolean hasCycle(com.keystone.graph.domain.model.Service service) {
        Set<UUID> visited = new HashSet<>();
        Set<UUID> recursionStack = new HashSet<>();
        return hasCycleDfs(service.getId(), visited, recursionStack);
    }

    /**
     * DFS-based cycle detection.
     */
    private boolean hasCycleDfs(UUID serviceId, Set<UUID> visited, Set<UUID> recursionStack) {
        if (recursionStack.contains(serviceId)) {
            return true; // cycle detected
        }
        if (visited.contains(serviceId)) {
            return false; // already processed
        }

        visited.add(serviceId);
        recursionStack.add(serviceId);

        List<ApiDependency> consumers = graphRepository.findConsumers(serviceId);
        for (ApiDependency dep : consumers) {
            UUID consumerId = dep.getConsumerId();
            if (consumerId != null) {
                if (hasCycleDfs(consumerId, visited, recursionStack)) {
                    return true;
                }
            }
        }

        recursionStack.remove(serviceId);
        return false;
    }

    /**
     * Event listener: automatically triggers impact analysis when a breaking
     * change is reported from the Breaking Change Analysis context.
     */
    @EventListener
    public ImpactAnalysisResult onBreakingChangeReported(BreakingChangeReportedEvent event) {
        log.info(
                "Received BreakingChangeReportedEvent for spec '{}' (reportId: {})",
                event.specPath(),
                event.reportId());

        ImpactAnalysisResult result = computeImpact(event.specPath(), event.reportId());

        log.info(
                "Impact analysis triggered by event: {} affected services",
                result.getAffectedServices().size());
        return result;
    }

    /**
     * Internal BFS node tracking service, path, and traversal depth.
     */
    private record BfsNode(com.keystone.graph.domain.model.Service service, String path, int depth) {}
}
