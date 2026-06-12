package com.keystone.graph.application.dto;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Output DTO for the service registration endpoint.
 *
 * <p>Returned by the {@link com.keystone.graph.interfaces.http.GraphController}
 * after successfully registering or updating a service declaration.
 *
 * @param serviceId   The UUID of the registered service
 * @param serviceName  The name of the registered service
 * @param producerCount Number of produced API specs registered
 * @param consumerCount Number of consumed API specs registered
 * @param createdAt   Timestamp of when the service was first created
 * @param updatedAt   Timestamp of the most recent update
 */
public record ServiceRegistrationResponse(
        UUID serviceId,
        String serviceName,
        int producerCount,
        int consumerCount,
        Instant createdAt,
        Instant updatedAt) {
    public ServiceRegistrationResponse {
        Objects.requireNonNull(serviceId, "serviceId must not be null");
        Objects.requireNonNull(serviceName, "serviceName must not be null");
        Objects.requireNonNull(createdAt, "createdAt must not be null");
        Objects.requireNonNull(updatedAt, "updatedAt must not be null");
    }
}
