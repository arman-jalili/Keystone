package com.keystone.graph.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.Objects;
import java.util.UUID;

/**
 * Input DTO for requesting an impact analysis.
 *
 * <p>Received by the {@link com.keystone.graph.interfaces.http.GraphController}
 * endpoint. Triggers BFS traversal to find all downstream services affected
 * by a breaking change to the specified spec.
 *
 * @param specPath           The spec path of the changed API (e.g. "openapi/checkout.yaml")
 * @param reportId           The ID of the breaking change report that triggered this analysis
 * @param producerServiceId  Optional — if specified, scopes the traversal to start
 *                           from a specific producer service rather than all producers
 */
public record ImpactAnalysisRequest(
        @NotBlank(message = "specPath is required")
                @Size(max = 512, message = "specPath must not exceed 512 characters")
                String specPath,
        UUID reportId,
        UUID producerServiceId) {
    public ImpactAnalysisRequest {
        Objects.requireNonNull(specPath, "specPath must not be null");
    }
}
