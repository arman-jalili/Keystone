package com.keystone.graph.application.service;

import com.keystone.graph.application.dto.ImpactAnalysisRequest;
import com.keystone.graph.application.dto.ImpactAnalysisResponse;
import com.keystone.graph.application.dto.ServiceRegistrationRequest;
import com.keystone.graph.application.dto.ServiceRegistrationResponse;
import com.keystone.graph.domain.exception.UnknownServiceException;
import com.keystone.graph.domain.model.ApiDependency;
import com.keystone.graph.domain.model.ImpactAnalysisResult;
import com.keystone.graph.domain.model.ServiceDeclaration;
import com.keystone.graph.domain.model.ServiceDeclaration.SpecConsumed;
import com.keystone.graph.domain.model.ServiceDeclaration.SpecProduced;
import com.keystone.graph.domain.service.DependencyParser;
import com.keystone.graph.domain.service.ImpactAnalyzer;
import com.keystone.graph.infrastructure.repository.GraphRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link GraphService}.
 *
 * <p>Orchestrates the dependency graph use cases:
 * <ul>
 *   <li>Service registration via {@link DependencyParser}</li>
 *   <li>Impact analysis via {@link ImpactAnalyzer}</li>
 *   <li>Graph queries via {@link GraphRepository}</li>
 * </ul>
 */
@Service
@Transactional
public class GraphServiceImpl implements GraphService {

    private static final Logger log = LoggerFactory.getLogger(GraphServiceImpl.class);

    private final ImpactAnalyzer impactAnalyzer;
    private final DependencyParser dependencyParser;
    private final GraphRepository graphRepository;

    public GraphServiceImpl(
            ImpactAnalyzer impactAnalyzer, DependencyParser dependencyParser, GraphRepository graphRepository) {
        this.impactAnalyzer = impactAnalyzer;
        this.dependencyParser = dependencyParser;
        this.graphRepository = graphRepository;
    }

    @Override
    public ServiceRegistrationResponse registerService(ServiceRegistrationRequest request) {
        log.info("Registering service '{}'", request.name());

        ServiceDeclaration declaration = new ServiceDeclaration(
                request.name(),
                request.team(),
                request.produces() != null
                        ? request.produces().stream()
                                .map(p -> new SpecProduced(p.specPath(), p.version()))
                                .collect(Collectors.toList())
                        : List.of(),
                request.consumes() != null
                        ? request.consumes().stream()
                                .map(c -> new SpecConsumed(c.serviceName(), c.specPath()))
                                .collect(Collectors.toList())
                        : List.of());

        dependencyParser.registerService(declaration);

        com.keystone.graph.domain.model.Service service = graphRepository
                .findServiceByName(request.name())
                .orElseThrow(() -> new UnknownServiceException(
                        "Service '" + request.name() + "' was not found after registration"));

        return toServiceResponse(service);
    }

    @Override
    public ImpactAnalysisResponse analyzeImpact(ImpactAnalysisRequest request) {
        log.info("Analyzing impact for spec '{}'", request.specPath());

        ImpactAnalysisResult result;
        if (request.producerServiceId() != null) {
            result = impactAnalyzer.computeImpactFromProducer(
                    request.specPath(), request.producerServiceId(), request.reportId());
        } else {
            result = impactAnalyzer.computeImpact(request.specPath(), request.reportId());
        }

        return new ImpactAnalysisResponse(
                result.getReportId(),
                request.specPath(),
                result.getAffectedServices().size(),
                result.getAffectedServices(),
                Instant.now());
    }

    @Override
    public ServiceRegistrationResponse getService(UUID serviceId) {
        return graphRepository
                .findServiceById(serviceId)
                .map(this::toServiceResponse)
                .orElse(null);
    }

    @Override
    public void removeService(String serviceName) {
        log.info("Removing service '{}'", serviceName);
        graphRepository.findServiceByName(serviceName).ifPresent(service -> {
            graphRepository.deleteService(service.getId());
        });
    }

    @Override
    public List<ServiceRegistrationResponse> listServices() {
        return graphRepository.findAllServices().stream()
                .map(this::toServiceResponse)
                .collect(Collectors.toList());
    }

    private ServiceRegistrationResponse toServiceResponse(com.keystone.graph.domain.model.Service service) {
        List<ApiDependency> deps = graphRepository.findDependencies(service.getId());
        List<ApiDependency> consumers = graphRepository.findConsumers(service.getId());
        return new ServiceRegistrationResponse(
                service.getId(),
                service.getName(),
                consumers.size(),
                deps.size(),
                service.getCreatedAt(),
                service.getUpdatedAt());
    }
}
