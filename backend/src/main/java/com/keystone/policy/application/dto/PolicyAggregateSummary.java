// Canonical Reference: .pi/architecture/modules/policy-engine.md
// Module: policy-engine
package com.keystone.policy.application.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response DTO for the aggregated policy summary endpoint.
 *
 * <p>Returned by {@code GET /api/v1/policies/summary}.
 * Provides a quick overview of policy health without fetching all policies.
 *
 * @param activePolicies  Number of currently active policies
 * @param passRate        Percentage of specs passing all policies (0–100)
 * @param openViolations  Number of unresolved violations
 * @param coveredApis     Number of APIs covered by at least one policy
 */
public record PolicyAggregateSummary(
        @JsonProperty("active_policies") int activePolicies,
        @JsonProperty("pass_rate") int passRate,
        @JsonProperty("open_violations") int openViolations,
        @JsonProperty("covered_apis") int coveredApis) {}
