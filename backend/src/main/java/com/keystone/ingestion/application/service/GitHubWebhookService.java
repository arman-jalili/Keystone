// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.application.service;

import com.keystone.ingestion.domain.exception.WebhookValidationException;
import java.util.UUID;

/**
 * Service interface for processing GitHub webhook events.
 *
 * <p>Validates the webhook signature, extracts the repository and spec
 * information from the push event payload, and queues the changed specs
 * for ingestion.
 */
public interface GitHubWebhookService {

    /**
     * Processes a GitHub push webhook event.
     *
     * @param payload          the raw JSON payload from the webhook
     * @param signature        the X-Hub-Signature-256 header value
     * @return a delivery ID that can be used to track this webhook event
     * @throws WebhookValidationException if the signature is invalid or the payload is malformed
     */
    UUID processWebhook(String payload, String signature) throws WebhookValidationException;
}
