// Canonical Reference: .pi/architecture/modules/contract-ingestion.md
// Module: contract-ingestion
package com.keystone.ingestion.domain.exception;

/**
 * Thrown when a webhook payload fails validation.
 *
 * <p>This includes invalid HMAC signatures, malformed JSON payloads,
 * or unrecognized event types that cannot be processed.
 */
public class WebhookValidationException extends RuntimeException {

    private final String deliveryId;
    private final String reason;

    public WebhookValidationException(String deliveryId, String reason) {
        super("Webhook validation failed for delivery " + deliveryId + ": " + reason);
        this.deliveryId = deliveryId;
        this.reason = reason;
    }

    public WebhookValidationException(String deliveryId, String reason, Throwable cause) {
        super("Webhook validation failed for delivery " + deliveryId + ": " + reason, cause);
        this.deliveryId = deliveryId;
        this.reason = reason;
    }

    public String getDeliveryId() {
        return deliveryId;
    }

    public String getReason() {
        return reason;
    }
}
