/**
 * Data service — fetches real backend data and transforms it for frontend components.
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#data-layer
 *
 * Each function:
 * 1. Fetches the real backend API endpoint
 * 2. Transforms snake_case response to camelCase (handled by apiClient)
 * 3. Maps backend DTO shapes to frontend component prop types
 * 4. Returns clean fallback data (empty/zero) when backend is unreachable
 *
 * NO MOCK DATA — all fetches go to the real backend.
 */
import { apiClient } from './client';
import type {
  GovernanceHealth,
  BreakingChangeSummary,
  PolicySummary,
  Notification,
  NotificationChannel,
  ApiInventoryItem,
  StaleApiItem,
  DependencyGraphData,
  ImpactCascade,
} from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// Helpers
// ──────────────────────────────────────────────

/**
 * Safe fetch wrapper — catches errors and returns fallback data.
 * Always logs errors so production operators can see backend failures.
 */
async function safeFetch<T>(fetchFn: () => Promise<T>, fallback: T): Promise<T> {
  try {
    return await fetchFn();
  } catch (err) {
    console.error('[API] Backend request failed, returning empty fallback:', (err as Error).message);
    return fallback;
  }
}

/** Zeroed health dimensions for empty state. */
function zeroDimensions(): GovernanceHealth['dimensions'] {
  return {
    compliance: { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
    breaking:   { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
    coverage:   { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
    staleness:  { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
    impact:     { value: 0, pct: 0, label: 'No data', trend: 'stable', tone: 'neutral' },
  };
}

// ──────────────────────────────────────────────
// Backend endpoint to frontend type mapping
//
// All functions call the real backend and map responses
// to frontend component prop types.
// ──────────────────────────────────────────────

/**
 * Build GovernanceHealth from available backend endpoints.
 * Composes /dashboard/summary + /dashboard/health-score.
 */
export async function fetchGovernanceHealth(): Promise<GovernanceHealth> {
  return safeFetch(async () => {
    const [summary, healthScore] = await Promise.allSettled([
      apiClient.get<any>('/dashboard/summary'),
      apiClient.get<any>('/dashboard/health-score?period=LAST_30_DAYS'),
    ]);

    const dimensions = zeroDimensions();
    let overallScore = 0;
    let totalApis = 0;
    let activePolicies = 0;
    let breakingChanges30d = 0;

    if (healthScore.status === 'fulfilled') {
      const hs = healthScore.value;
      const compliancePct = Math.round((hs.policyPassRate ?? 0) * 100);
      const coveragePct = Math.round((hs.specComplianceRate ?? 0) * 100);
      dimensions.compliance = {
        value: hs.policyPassRate ?? 0, pct: compliancePct,
        label: `${compliancePct}% pass rate`, trend: 'stable', tone: 'success',
      };
      dimensions.coverage = {
        value: hs.specComplianceRate ?? 0, pct: coveragePct,
        label: `${coveragePct}% compliance`, trend: 'stable', tone: 'accent',
      };
      overallScore = Math.round((hs.score ?? 0) * 100);
    }

    if (summary.status === 'fulfilled') {
      const s = summary.value;
      totalApis = s.totalSpecs ?? 0;
      activePolicies = s.activePolicies ?? 0;
      breakingChanges30d = s.recentViolations ?? 0;
      if (!healthScore || overallScore === 0) {
        overallScore = Math.round((s.overallScore ?? 0) * 100);
      }
    }

    return {
      overallScore,
      dimensions,
      summary: {
        totalApis,
        activePolicies,
        breakingChanges30d,
        servicesAtRisk: 0,
        dependencyEdges: 0,
      },
      recentBreakages: [],
      topViolations: [],
    };
  }, {
    overallScore: 0,
    dimensions: zeroDimensions(),
    summary: { totalApis: 0, activePolicies: 0, breakingChanges30d: 0, servicesAtRisk: 0, dependencyEdges: 0 },
    recentBreakages: [],
    topViolations: [],
  });
}

/**
 * Fetch breaking changes from the backend.
 * Calls GET /breaking/reports/latest — this endpoint needs to be added to the backend.
 * Returns clean empty data until the endpoint is available.
 */
export async function fetchBreakingChanges(): Promise<BreakingChangeSummary> {
  return safeFetch(async () => {
    const reports = await apiClient.get<any[]>('/breaking/reports/latest?limit=50');
    const critical = reports.filter((r: any) => r.verdict === 'BREAKING').length;
    const high = reports.filter((r: any) => r.verdict === 'BREAKING').length;
    const nonBreaking = reports.filter((r: any) => r.verdict === 'NON_BREAKING').length;

    return {
      total30d: reports.length,
      critical,
      high,
      nonBreaking,
      items: reports.map((r: any) => ({
        id: r.analysisId ?? `r-${Math.random().toString(36).slice(2)}`,
        serviceName: r.repository ?? 'unknown',
        changeType: 'field-removal' as const,
        severity: r.verdict === 'BREAKING' ? 'critical' as const : 'high' as const,
        detectedAt: r.completedAt ?? new Date().toISOString(),
        versionFrom: r.baseVersion ?? '',
        versionTo: r.targetVersion ?? '',
        diffText: '-  breaking change detected',
        impactedConsumers: [],
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
    const violated = policies.filter((p: any) => p.status === 'VIOLATED').length;
    return {
      activePolicies: active,
      passRate: active > 0 ? Math.round(((active - violated) / active) * 100) : 0,
      openViolations: violated,
      coveredApis: policies.length,
      policies: policies.map((p: any) => ({
        id: p.id,
        name: p.name,
        description: p.description ?? '',
        scope: p.sourceId ?? 'all APIs',
        status: (p.status === 'ACTIVE' || p.status === 'passing') ? 'passing' as const : 'violated' as const,
        violationCount: p.violationCount ?? 0,
        violatingServices: [],
      })),
    };
  }, { activePolicies: 0, passRate: 0, openViolations: 0, coveredApis: 0, policies: [] });
}

/**
/**
 * API Inventory — from GET /ingestion/apis.
 * Maps backend ApiInventoryItem → frontend ApiInventoryItem[]
 * (snake_case → camelCase handled by apiClient automatically).
 */
export async function fetchApiInventory(): Promise<ApiInventoryItem[]> {
  return safeFetch(async () => {
    const items = await apiClient.get<any[]>('/ingestion/apis');
    return items.map((item: any) => ({
      id: item.id,
      serviceName: item.serviceName,
      version: item.version ?? '',
      specFormat: item.specFormat === 'OpenAPI 3.1' ? 'OpenAPI 3.1' as const : 'OpenAPI 3.0' as const,
      health: (item.health === 'healthy' || item.health === 'low-risk' || item.health === 'warning' || item.health === 'at-risk')
        ? item.health as 'healthy' | 'low-risk' | 'warning' | 'at-risk'
        : item.health === 'healthy' ? 'healthy' : 'warning',
      lastAnalyzed: item.lastAnalyzed ?? '',
      owner: item.owner ?? '',
      policyPassRate: item.policyPassRate ?? undefined,
      openBreakages: item.openBreakages ?? undefined,
    }));
  }, []);
}

/**
 * Stale APIs — from GET /ingestion/apis/stale?threshold_days=30.
 * Maps backend StaleApiItem → frontend StaleApiItem[].
 */
export async function fetchStaleApis(): Promise<StaleApiItem[]> {
  return safeFetch(async () => {
    const staleList = await apiClient.get<any[]>('/ingestion/apis/stale?thresholdDays=30');
    return staleList.map((s: any) => ({
      id: s.id,
      serviceName: s.serviceName ?? 'unknown',
      lastIngested: s.lastIngested ?? '',
      daysStale: s.daysStale ?? 0,
      version: s.version ?? '',
    }));
  }, []);
}

/**
 * Dependency Graph — from GET /graph/services.
 * Maps ServiceRegistrationResponse → DependencyGraphData.
 */
export async function fetchDependencyGraph(): Promise<DependencyGraphData> {
  return safeFetch(async () => {
    const services = await apiClient.get<any[]>('/graph/services');
    const nodes = services.map((s: any) => ({
      id: s.serviceName ?? s.serviceId,
      label: s.serviceName ?? 'unknown',
      subtitle: `${s.producerCount ?? 0} producers · ${s.consumerCount ?? 0} consumers`,
      kind: 'svc' as const,
      x: 0, y: 0,
      impacted: false,
    }));
    return { nodes, edges: [] };
  }, { nodes: [], edges: [] });
}

/**
 * Impact cascades — calls POST /graph/impact.
 * This endpoint requires a service name to compute cascades against.
 */
export async function fetchImpactCascades(): Promise<ImpactCascade[]> {
  return safeFetch(async () => {
    // With no specific service selected, return empty cascades.
    // The graph view should call this with a specific service name when a node is selected.
    const result = await apiClient.post<any>('/graph/impact', { serviceName: '' });
    return (result.cascades ?? []).map((c: any) => ({
      id: c.id ?? `c-${Math.random().toString(36).slice(2)}`,
      sourceService: c.sourceService ?? '',
      sourceVersion: c.sourceVersion ?? '',
      changeDescription: c.changeDescription ?? '',
      downstreamServices: c.downstreamServices ?? [],
      totalConsumers: c.totalConsumers ?? 0,
      severity: (c.severity === 'critical' || c.severity === 'high') ? c.severity : 'high' as const,
    }));
  }, []);
}

/**
 * Notifications — from GET /notifications.
 * Maps backend NotificationResponse → frontend Notification[].
 * Backend returns: notificationId, channelName, channelId, status (PENDING/DELIVERED/FAILED/RETRYING), message, payloadType, createdAt
 */
export async function fetchNotifications(): Promise<Notification[]> {
  return safeFetch(async () => {
    const items = await apiClient.get<any[]>('/notifications?limit=50&unreadFirst=true');
    return items.map((n: any) => ({
      id: n.notificationId ?? n.id,
      title: n.payloadType ?? 'Notification',
      description: n.message ?? '',
      severity: n.status === 'FAILED' ? 'critical' as const : n.status === 'RETRYING' ? 'high' as const : 'warning' as const,
      channel: 'webhook' as const,
      channelDetail: n.channelName ?? '',
      read: n.status === 'DELIVERED',
      timestamp: n.createdAt ?? new Date().toISOString(),
      relativeTime: n.createdAt ? timeAgo(new Date(n.createdAt)) : '',
    }));
  }, []);
}

/** Simple relative time helper. */
function timeAgo(date: Date): string {
  const seconds = Math.floor((Date.now() - date.getTime()) / 1000);
  if (seconds < 60) return 'just now';
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days}d ago`;
  return `${Math.floor(days / 30)}mo ago`;
}

/**
 * Notification channels — from GET /notifications/channels.
 * Maps ChannelStatusResponse → NotificationChannel[].
 */
export async function fetchNotificationChannels(): Promise<NotificationChannel[]> {
  return safeFetch(async () => {
    const status = await apiClient.get<any>('/notifications/channels');
    const channelList = status.channels ?? [];
    return channelList.map((c: any) => ({
      id: c.name ?? c.type ?? `ch-${Math.random().toString(36).slice(2)}`,
      type: 'webhook' as const,
      status: c.available ? 'active' as const : 'inactive' as const,
      config: {
        target: c.name ?? '',
        rules: [],
        lastDelivered: '',
      },
    }));
  }, []);
}
