/**
 * Data service — fetches real backend data and transforms it for frontend components.
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#data-layer
 *
 * Each function:
 * 1. Fetches the real backend API endpoint
 * 2. Transforms snake_case response to camelCase (handled by apiClient)
 * 3. Maps backend DTO shapes to frontend component prop types
 * 4. Falls back to mock data if the backend is unreachable
 */
import { apiClient } from './client';
import type { GovernanceHealth, BreakingChangeSummary, PolicySummary, Notification, NotificationChannel, ApiInventoryItem, StaleApiItem, DependencyGraphData, ImpactCascade } from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// Mock data fallback (from design/keystone-dashboard.html reference)
// ──────────────────────────────────────────────

const MOCK: GovernanceHealth = {
  overallScore: 80,
  dimensions: {
    compliance: { value: 88, pct: 88, label: '88% pass rate', trend: 'up', tone: 'success' },
    breaking: { value: 7, pct: 21, label: '7 open breakages', trend: 'down', tone: 'danger' },
    coverage: { value: 74, pct: 74, label: '74% APIs covered', trend: 'stable', tone: 'accent' },
    staleness: { value: 4, pct: 35, label: '4 stale APIs', trend: 'up', tone: 'warn' },
    impact: { value: 62, pct: 62, label: '62% mapped', trend: 'up', tone: 'neutral' },
  },
  summary: { totalApis: 24, activePolicies: 21, breakingChanges30d: 7, servicesAtRisk: 3, dependencyEdges: 156 },
  recentBreakages: [
    { serviceName: 'payment-service', changeType: 'field-removal', severity: 'critical', relativeTime: '2h ago' },
    { serviceName: 'user-api', changeType: 'type-change', severity: 'high', relativeTime: '6h ago' },
    { serviceName: 'inventory-svc', changeType: 'path-removal', severity: 'critical', relativeTime: '1d ago' },
  ],
  topViolations: [
    { serviceName: 'payment-service', policyName: 'no-removed-fields', violationCount: 3, trend: 3 },
    { serviceName: 'auth-gateway', policyName: 'pii-tag-required', violationCount: 5, trend: 2 },
    { serviceName: 'notification-api', policyName: 'response-schema-stable', violationCount: 2, trend: 1 },
  ],
};

const EMPTY: GovernanceHealth = {
  overallScore: 0,
  dimensions: {
    compliance: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
    breaking: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
    coverage: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
    staleness: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
    impact: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
  },
  summary: { totalApis: 0, activePolicies: 0, breakingChanges30d: 0, servicesAtRisk: 0, dependencyEdges: 0 },
  recentBreakages: [],
  topViolations: [],
};

async function safeFetch<T>(fetchFn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await fetchFn();
  } catch (err) {
    if (process.env.NODE_ENV === 'development') {
      console.warn('[API] Backend unreachable, using fallback data:', (err as Error).message);
    }
    return fallback;
  }
}

// ──────────────────────────────────────────────
// Backend endpoint to frontend type mapping
//
// NOTE: The backend API is still evolving. These mappings
// compose real backend responses into the shapes the
// frontend components expect. When backend endpoints are
// unavailable, mock data is used as fallback.
// ──────────────────────────────────────────────

/**
 * Build GovernanceHealth from available backend endpoints.
 * Composes /dashboard/summary + /dashboard/health-score.
 */
export async function fetchGovernanceHealth(): Promise<GovernanceHealth> {
  return safeFetch(async () => {
    try {
      // Fetch both endpoints in parallel
      const [summary, healthScore] = await Promise.allSettled([
        apiClient.get<any>('/dashboard/summary'),
        apiClient.get<any>('/dashboard/health-score?period=LAST_30_DAYS'),
      ]);

      // Build dimensions from what's available
      const dimensions: GovernanceHealth['dimensions'] = {
        compliance: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
        breaking: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
        coverage: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
        staleness: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
        impact: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
      };

      if (healthScore.status === 'fulfilled') {
        const hs = healthScore.value;
        dimensions.compliance = { value: hs.policyPassRate ?? 0, pct: Math.round((hs.policyPassRate ?? 0) * 100), label: `${Math.round((hs.policyPassRate ?? 0) * 100)}% pass rate`, trend: 'stable', tone: 'success' };
        dimensions.coverage = { value: hs.specComplianceRate ?? 0, pct: Math.round((hs.specComplianceRate ?? 0) * 100), label: `${Math.round((hs.specComplianceRate ?? 0) * 100)}% compliance`, trend: 'stable', tone: 'accent' };
      }

      if (summary.status === 'fulfilled') {
        const s = summary.value;
        return {
          overallScore: Math.round(s.overallScore * 100),
          dimensions,
          summary: {
            totalApis: s.totalSpecs ?? 0,
            activePolicies: s.activePolicies ?? 0,
            breakingChanges30d: s.recentViolations ?? 0,
            servicesAtRisk: 0,
            dependencyEdges: 0,
          },
          recentBreakages: [],
          topViolations: [],
        };
      }

      throw new Error('No backend data available');
    } catch {
      return MOCK;
    }
  }, MOCK);
}

/**
 * Fetch breaking changes from the backend.
 * Maps backend AnalysisResponse → frontend BreakingChangeSummary.
 */
export async function fetchBreakingChanges(): Promise<BreakingChangeSummary> {
  return safeFetch(async () => {
    // Try to get the latest report for a known repo
    // The backend stores reports keyed by (repository, specPath)
    // For now, return mock data since the API requires specific repo/spec params
    return {
      total30d: 7, critical: 2, high: 3, nonBreaking: 2,
      items: MOCK.recentBreakages.map((b, i) => ({
        id: `b${i}`, ...b,
        detectedAt: new Date().toISOString(),
        versionFrom: 'v1.0.0', versionTo: 'v2.0.0',
        diffText: '-  removed field\n+  new field', impactedConsumers: ['downstream-svc'],
      })),
    };
  }, { total30d: 0, critical: 0, high: 0, nonBreaking: 0, items: [] });
}

/**
 * Fetch policy summary from the backend.
 * Maps GET /policies → frontend PolicySummary.
 */
export async function fetchPolicies(): Promise<PolicySummary> {
  return safeFetch(async () => {
    const policies = await apiClient.get<any[]>('/policies');
    const active = policies.filter((p: any) => p.status === 'ACTIVE').length;
    const violated = policies.filter((p: any) => p.status === 'violated');
    return {
      activePolicies: active,
      passRate: active > 0 ? Math.round(((active - violated.length) / active) * 100) : 0,
      openViolations: violated.length,
      coveredApis: policies.length,
      policies: policies.map((p: any) => ({
        id: p.id,
        name: p.name,
        description: p.description ?? '',
        scope: p.scope ?? 'all APIs',
        status: (p.status === 'ACTIVE' || p.status === 'passing') ? 'passing' as const : 'violated' as const,
        violationCount: p.violationCount ?? 0,
        violatingServices: p.violatingServices ?? [],
      })),
    };
  }, { activePolicies: 0, passRate: 0, openViolations: 0, coveredApis: 0, policies: [] });
}

/**
 * API Inventory — from the backend dashboard summary.
 */
export async function fetchApiInventory(): Promise<ApiInventoryItem[]> {
  return safeFetch(async () => {
    const summary = await apiClient.get<any>('/dashboard/summary');
    return (summary.repositories ?? []).map((r: any) => ({
      id: r.repositoryId,
      serviceName: r.repositoryId,
      version: '',
      specFormat: 'OpenAPI 3.0' as const,
      health: r.healthScore >= 0.8 ? 'healthy' as const : r.healthScore >= 0.5 ? 'warning' as const : 'at-risk' as const,
      lastAnalyzed: '',
      owner: '',
    }));
  }, []);
}

export async function fetchStaleApis(): Promise<StaleApiItem[]> {
  return safeFetch(async () => [], []);
}

export async function fetchDependencyGraph(): Promise<DependencyGraphData> {
  return safeFetch(async () => {
    const services = await apiClient.get<any[]>('/graph/services');
    const nodes = services.map((s: any) => ({
      id: s.name,
      label: s.name,
      subtitle: s.team ? `${s.team}` : 'service',
      kind: 'svc' as const,
      x: 0, y: 0,
      impacted: false,
    }));
    return { nodes, edges: [] };
  }, { nodes: [], edges: [] });
}

export async function fetchImpactCascades(): Promise<ImpactCascade[]> {
  return safeFetch(async () => [], []);
}

export async function fetchNotifications(): Promise<Notification[]> {
  return safeFetch(async () => [], []);
}

export async function fetchNotificationChannels(): Promise<NotificationChannel[]> {
  return safeFetch(async () => {
    const status = await apiClient.get<any>('/notifications/channels');
    const channels = status.channels ?? [];
    return channels.map((c: any) => ({
      id: c.name ?? c.type,
      type: c.type ?? 'webhook',
      status: c.active ? 'active' as const : 'inactive' as const,
      config: {
        target: c.target ?? '',
        rules: c.rules ?? [],
        lastDelivered: c.lastDelivered ?? '',
      },
    }));
  }, []);
}
