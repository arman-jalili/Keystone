// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.evaluator;

import com.keystone.analysis.domain.event.BreakingChangeReportedEvent;
import com.keystone.policy.application.dto.EvaluateSpecRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Listens for {@link BreakingChangeReportedEvent} from the Breaking Change
 * Analysis module and triggers policy evaluation on the affected spec.
 *
 * <p>This connects the analysis pipeline to the policy engine:
 * when a breaking change analysis completes, the spec is automatically
 * evaluated against active policies.
 */
@Component
public class BreakingChangeReportListener {

    private static final Logger log = LoggerFactory.getLogger(BreakingChangeReportListener.class);

    private final PolicyEvaluationServiceImpl evaluationService;

    public BreakingChangeReportListener(PolicyEvaluationServiceImpl evaluationService) {
        this.evaluationService = evaluationService;
    }

    /**
     * Handles a completed breaking change analysis by triggering
     * policy evaluation on the affected specification.
     *
     * @param event the breaking change report completed event
     */
    @EventListener
    public void onBreakingChangeReported(BreakingChangeReportedEvent event) {
        log.info(
                "Breaking change analysis completed for {}/{}, verdict={}. Triggering policy evaluation.",
                event.repository(),
                event.specPath(),
                event.verdict());

        try {
            var request = new EvaluateSpecRequest(
                    event.repository(),
                    event.specPath(),
                    event.idempotencyKey().length() >= 40
                            ? event.idempotencyKey().substring(0, 40)
                            : event.idempotencyKey(),
                    null);

            var response = evaluationService.evaluateSpec(request);
            log.info(
                    "Policy evaluation completed for {}/{}: verdict={}, violations={}",
                    event.repository(),
                    event.specPath(),
                    response.verdict(),
                    response.violations().size());

        } catch (Exception e) {
            log.error(
                    "Failed to evaluate policies for {}/{}: {}",
                    event.repository(),
                    event.specPath(),
                    e.getMessage(),
                    e);
        }
    }
}
