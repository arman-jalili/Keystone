package com.keystone.dashboard.application.service;

import com.keystone.dashboard.application.dto.PolicyBreakdownResponse;
import com.keystone.dashboard.application.dto.PolicyFilterRequest;
import com.keystone.dashboard.application.dto.PolicySummaryResponse;
import java.util.List;

/**
 * Application service interface for the Policy UI use case.
 *
 * <p>This is the primary inbound port (driving adapter) for the
 * policy UI component. It defines the contract that the
 * {@link com.keystone.dashboard.interfaces.http.DashboardController} depends on
 * for policy-related dashboard views.
 *
 * <p>Provides read-optimized access to policy data for the dashboard
 * frontend. This is a separate concern from the policy management and
 * evaluation services in the policy module.
 */
public interface PolicyUiService {

    /**
     * Retrieves a paginated, filterable list of policies for the UI.
     *
     * @param request filter and pagination parameters
     * @return list of policy summaries matching the filter
     */
    List<PolicySummaryResponse> listPolicies(PolicyFilterRequest request);

    /**
     * Retrieves the policy breakdown by status and severity.
     *
     * <p>Provides aggregate data for charts and summary widgets.
     *
     * @return policy breakdown data
     */
    PolicyBreakdownResponse getPolicyBreakdown();

    /**
     * Retrieves a single policy summary by ID.
     *
     * @param policyId the policy UUID
     * @return the policy summary
     * @throws com.keystone.dashboard.domain.exception.DashboardDataNotFoundException
     *         if the policy is not found
     */
    PolicySummaryResponse getPolicy(String policyId)
            throws com.keystone.dashboard.domain.exception.DashboardDataNotFoundException;
}
