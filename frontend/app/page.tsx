'use client';

import { useState, useCallback } from 'react';
import { AppLayout } from '@/components/layout/AppLayout';
import { NavRail } from '@/components/layout/NavRail';
import { isValidViewId, DEFAULT_VIEW, VIEW_REGISTRY, NAV_ITEMS } from '@/lib/contracts/view-config';
import type { ViewId } from '@/lib/contracts/types';
import { ViewShell } from '@/components/shared/ViewShell';
import { OverviewView } from '@/components/overview/OverviewView';
import { InventoryView } from '@/components/inventory/InventoryView';
import { BreakingView } from '@/components/breaking/BreakingView';
import { PolicyView } from '@/components/policy/PolicyView';
import { GraphView } from '@/components/graph/GraphView';
import { NotificationsView } from '@/components/notifications/NotificationsView';

/**
 * Mock data for development — matches design/keystone-dashboard.html reference.
 * @see lib/contracts/api-client.ts for the ViewDataService contract.
 */
const MOCK_DATA = {
  overview: {
    overallScore: 80,
    dimensions: {
      compliance: { value: 88, pct: 88, label: '88% pass rate', trend: 'up' as const, tone: 'success' as const },
      breaking: { value: 7, pct: 21, label: '7 open breakages', trend: 'down' as const, tone: 'danger' as const },
      coverage: { value: 74, pct: 74, label: '74% APIs covered', trend: 'stable' as const, tone: 'accent' as const },
      staleness: { value: 4, pct: 35, label: '4 stale APIs', trend: 'up' as const, tone: 'warn' as const },
      impact: { value: 62, pct: 62, label: '62% mapped', trend: 'up' as const, tone: 'fg' as const },
    },
    summary: {
      totalApis: 24,
      activePolicies: 21,
      breakingChanges30d: 7,
      servicesAtRisk: 3,
      dependencyEdges: 156,
    },
    recentBreakages: [
      { serviceName: 'payment-service', changeType: 'field-removal', severity: 'critical' as const, relativeTime: '2h ago' },
      { serviceName: 'user-api', changeType: 'type-change', severity: 'high' as const, relativeTime: '6h ago' },
      { serviceName: 'inventory-svc', changeType: 'path-removal', severity: 'critical' as const, relativeTime: '1d ago' },
    ],
    topViolations: [
      { serviceName: 'payment-service', policyName: 'no-removed-fields', violationCount: 3, trend: 3 },
      { serviceName: 'auth-gateway', policyName: 'pii-tag-required', violationCount: 5, trend: 2 },
      { serviceName: 'notification-api', policyName: 'response-schema-stable', violationCount: 2, trend: 1 },
    ],
  },
  apis: [
    { id: '1', serviceName: 'payment-service', version: '3.2.1', specFormat: 'OpenAPI 3.0' as const, health: 'at-risk' as const, lastAnalyzed: '2026-06-13 14:22', owner: 'payments-team' },
    { id: '2', serviceName: 'user-api', version: '2.8.0', specFormat: 'OpenAPI 3.1' as const, health: 'warning' as const, lastAnalyzed: '2026-06-13 11:05', owner: 'identity-team' },
    { id: '3', serviceName: 'inventory-svc', version: '1.4.2', specFormat: 'OpenAPI 3.0' as const, health: 'at-risk' as const, lastAnalyzed: '2026-06-12 18:30', owner: 'warehouse-team' },
    { id: '4', serviceName: 'auth-gateway', version: '4.1.0', specFormat: 'OpenAPI 3.1' as const, health: 'warning' as const, lastAnalyzed: '2026-06-12 09:15', owner: 'security-team' },
    { id: '5', serviceName: 'notification-api', version: '2.3.1', specFormat: 'OpenAPI 3.0' as const, health: 'warning' as const, lastAnalyzed: '2026-06-11 22:45', owner: 'comm-team' },
    { id: '6', serviceName: 'catalog-service', version: '5.0.0', specFormat: 'OpenAPI 3.1' as const, health: 'healthy' as const, lastAnalyzed: '2026-06-11 16:10', owner: 'catalog-team' },
    { id: '7', serviceName: 'billing-api', version: '2.1.3', specFormat: 'OpenAPI 3.0' as const, health: 'healthy' as const, lastAnalyzed: '2026-06-11 10:00', owner: 'billing-team' },
    { id: '8', serviceName: 'search-index', version: '1.9.0', specFormat: 'OpenAPI 3.1' as const, health: 'low-risk' as const, lastAnalyzed: '2026-06-10 14:30', owner: 'search-team' },
  ],
  staleApis: [
    { id: 's1', serviceName: 'legacy-gateway', lastIngested: '2026-05-01', daysStale: 43, version: '1.2.0' },
    { id: 's2', serviceName: 'reporting-api', lastIngested: '2026-05-18', daysStale: 26, version: '3.0.5' },
    { id: 's3', serviceName: 'audit-log', lastIngested: '2026-05-22', daysStale: 22, version: '2.7.1' },
    { id: 's4', serviceName: 'config-service', lastIngested: '2026-05-29', daysStale: 15, version: '1.5.0' },
  ],
  breakingSummary: {
    total30d: 7,
    critical: 3,
    high: 4,
    nonBreaking: 12,
    items: [
      {
        id: 'b1', serviceName: 'payment-service', changeType: 'field-removal' as const, severity: 'critical' as const,
        detectedAt: '2026-06-13 14:22', versionFrom: '3.2.0', versionTo: '3.2.1',
        diffText: '-   "amount": { "type": "number", "format": "decimal" }\n  "currency": { "type": "string", "enum": ["USD","EUR","GBP"] }\n+   "totalCents": { "type": "integer", "minimum": 0 }',
        impactedConsumers: ['checkout-ui', 'invoice-generator', 'mobile-app', 'analytics-pipeline'],
      },
      {
        id: 'b2', serviceName: 'inventory-svc', changeType: 'path-removal' as const, severity: 'critical' as const,
        detectedAt: '2026-06-12 18:30', versionFrom: '1.4.0', versionTo: '1.4.2',
        diffText: '- /stock/{id}\n-   get:\n-     summary: Get stock level by item ID\n-     parameters:\n-       - name: id\n-         in: path\n-         required: true\n-         schema: { type: string }\n+ /inventory/{itemId}:\n+   get:\n+     summary: Get inventory record\n+     parameters:\n+       - name: itemId\n+         in: path\n+         required: true\n+         schema: { type: string }',
        impactedConsumers: ['warehouse-portal', 'supplier-integration'],
      },
      {
        id: 'b3', serviceName: 'user-api', changeType: 'type-change' as const, severity: 'high' as const,
        detectedAt: '2026-06-13 11:05', versionFrom: '2.7.0', versionTo: '2.8.0',
        diffText: '-   "email": { "type": "string", "format": "email" }\n+   "email": { "type": ["string", "null"], "format": "email" }\n  "name": { "type": "string" }',
        impactedConsumers: ['admin-console', 'profile-service', 'audit-export'],
      },
    ],
  },
  policySummary: {
    activePolicies: 21,
    passRate: 88,
    openViolations: 12,
    coveredApis: 24,
    policies: [
      { id: 'p1', name: 'no-removed-fields', description: 'Forbids removal of any field from a response schema without a deprecation window of at least 30 days.', scope: 'all APIs', status: 'violated' as const, violationCount: 3, violatingServices: ['payment-service', 'inventory-svc', 'user-api'] },
      { id: 'p2', name: 'pii-tag-required', description: 'All fields containing personally identifiable information must carry x-pii: true in the OpenAPI spec.', scope: 'user-facing APIs', status: 'violated' as const, violationCount: 5, violatingServices: ['auth-gateway', 'user-api', 'profile-service'] },
      { id: 'p3', name: 'response-schema-stable', description: 'Response schemas marked x-stability: stable must not change their shape — additions are allowed, removals or renames are not.', scope: 'stable-only APIs', status: 'violated' as const, violationCount: 2, violatingServices: ['notification-api', 'search-index'] },
      { id: 'p4', name: 'path-naming-convention', description: 'All API paths must follow kebab-case and must not include version prefixes (version goes in the header).', scope: 'all APIs', status: 'violated' as const, violationCount: 2, violatingServices: ['legacy-gateway', 'reporting-api'] },
      { id: 'p5', name: 'required-descriptions', description: 'Every endpoint, parameter, and schema property must carry a non-empty description field.', scope: 'all APIs', status: 'passing' as const, violationCount: 0, violatingServices: [] },
      { id: 'p6', name: 'semantic-versioning', description: 'Spec version must follow semver. Breaking changes require a major bump; additions require a minor bump.', scope: 'all APIs', status: 'passing' as const, violationCount: 0, violatingServices: [] },
      { id: 'p7', name: 'rate-limit-headers', description: 'All endpoints must include X-RateLimit-Remaining and X-RateLimit-Reset in their response headers.', scope: 'public APIs', status: 'passing' as const, violationCount: 0, violatingServices: [] },
    ],
  },
  graph: {
    nodes: [
      { id: 'checkout-ui', label: 'checkout-ui', subtitle: 'SVC · v2.3', kind: 'svc' as const, x: 80, y: 85, impacted: false },
      { id: 'invoice-gen', label: 'invoice-gen', subtitle: 'SVC · v1.8', kind: 'svc' as const, x: 80, y: 210, impacted: false },
      { id: 'analytics-pipe', label: 'analytics-pipe', subtitle: 'SVC · v4.2', kind: 'svc' as const, x: 80, y: 335, impacted: false },
      { id: 'user-api', label: 'user-api', subtitle: 'API · v2.8.0', kind: 'api' as const, x: 280, y: 85, impacted: false },
      { id: 'auth-gateway', label: 'auth-gateway', subtitle: 'API · v4.1.0', kind: 'api' as const, x: 280, y: 210, impacted: false },
      { id: 'notification-api', label: 'notification-api', subtitle: 'API · v2.3.1', kind: 'api' as const, x: 280, y: 335, impacted: false },
      { id: 'payment-svc', label: 'payment-svc', subtitle: 'API · v3.2.1', kind: 'api' as const, x: 480, y: 85, impacted: true },
      { id: 'inventory-svc', label: 'inventory-svc', subtitle: 'API · v1.4.2', kind: 'api' as const, x: 480, y: 210, impacted: false },
      { id: 'catalog-svc', label: 'catalog-svc', subtitle: 'API · v5.0.0', kind: 'api' as const, x: 680, y: 85, impacted: false },
      { id: 'billing-api', label: 'billing-api', subtitle: 'API · v2.1.3', kind: 'api' as const, x: 680, y: 210, impacted: false },
      { id: 'search-index', label: 'search-index', subtitle: 'API · v1.9.0', kind: 'api' as const, x: 680, y: 335, impacted: false },
      { id: 'mobile-app', label: 'mobile-app', subtitle: 'UI', kind: 'svc' as const, x: 810, y: 85, impacted: false },
      { id: 'admin-console', label: 'admin-console', subtitle: 'UI', kind: 'svc' as const, x: 810, y: 210, impacted: false },
      { id: 'warehouse', label: 'warehouse', subtitle: 'UI', kind: 'svc' as const, x: 810, y: 335, impacted: false },
    ],
    edges: [
      { from: 'user-api', to: 'payment-svc', impacted: true },
      { from: 'auth-gateway', to: 'payment-svc', impacted: false },
      { from: 'auth-gateway', to: 'inventory-svc', impacted: false },
      { from: 'payment-svc', to: 'user-api', impacted: false },
      { from: 'inventory-svc', to: 'catalog-svc', impacted: false },
      { from: 'inventory-svc', to: 'billing-api', impacted: false },
      { from: 'inventory-svc', to: 'search-index', impacted: false },
      { from: 'catalog-svc', to: 'mobile-app', impacted: false },
      { from: 'billing-api', to: 'admin-console', impacted: false },
      { from: 'search-index', to: 'admin-console', impacted: false },
      { from: 'search-index', to: 'warehouse', impacted: false },
      { from: 'checkout-ui', to: 'user-api', impacted: false },
      { from: 'invoice-gen', to: 'auth-gateway', impacted: false },
      { from: 'analytics-pipe', to: 'auth-gateway', impacted: false },
      { from: 'analytics-pipe', to: 'notification-api', impacted: false },
    ],
  },
  cascades: [
    {
      id: 'c1', sourceService: 'payment-service', sourceVersion: 'v3.2.1',
      changeDescription: 'A field removal in payment-service v3.2.1 directly breaks checkout-ui, which consumes /order.amount. The change cascades through invoice-generator (reads from checkout-ui) and analytics-pipeline (reads from invoice-generator).',
      downstreamServices: ['checkout-ui', 'invoice-generator', 'analytics-pipeline'],
      totalConsumers: 4, severity: 'critical' as const,
    },
  ],
  notifications: [
    { id: 'n1', title: 'Breaking change detected — payment-service v3.2.1', description: 'Field /order.amount removed. 4 consumers impacted. CI status: FAIL.', severity: 'critical' as const, channel: 'slack' as const, channelDetail: '#api-gov', read: false, timestamp: '2026-06-13T10:30:00Z', relativeTime: '2 hours ago' },
    { id: 'n2', title: 'Policy violation — pii-tag-required on auth-gateway', description: '5 PII fields lack x-pii tags. Re-ingestion required within 48h.', severity: 'high' as const, channel: 'email' as const, channelDetail: 'gov-team@company.com', read: false, timestamp: '2026-06-13T09:00:00Z', relativeTime: '3 hours ago' },
    { id: 'n3', title: 'Path renamed — inventory-svc v1.4.2', description: '/stock/{id} renamed to /inventory/{itemId}. 2 consumers must update.', severity: 'critical' as const, channel: 'slack' as const, channelDetail: '#api-gov', read: false, timestamp: '2026-06-12T18:30:00Z', relativeTime: '1 day ago' },
    { id: 'n4', title: 'Staleness alert — legacy-gateway 43 days without ingestion', description: 'Spec last ingested 2026-05-01. Auto-removal from registry scheduled in 17 days.', severity: 'warning' as const, channel: 'email' as const, channelDetail: 'gov-team@company.com', read: false, timestamp: '2026-06-12T09:00:00Z', relativeTime: '1 day ago' },
  ],
  channels: [
    { id: 'ch1', type: 'slack' as const, status: 'active' as const, config: { target: '#api-gov', rules: ['on-breaking-change', 'on-policy-violation'], lastDelivered: '2 hours ago' } },
    { id: 'ch2', type: 'email' as const, status: 'active' as const, config: { target: 'gov-team@company.com', rules: ['on-breaking-change', 'on-staleness', 'weekly-summary'], lastDelivered: '3 hours ago' } },
  ],
};

function renderView(viewId: ViewId) {
  const def = VIEW_REGISTRY[viewId];

  switch (viewId) {
    case 'overview':
      return (
        <ViewShell title={def.title} subtitle={def.subtitle}>
          <OverviewView data={MOCK_DATA.overview as any} />
        </ViewShell>
      );
    case 'inventory':
      return (
        <ViewShell title={def.title} subtitle={def.subtitle}>
          <InventoryView apis={MOCK_DATA.apis as any} staleApis={MOCK_DATA.staleApis as any} />
        </ViewShell>
      );
    case 'breaking':
      return (
        <ViewShell title={def.title} subtitle={def.subtitle}>
          <BreakingView data={MOCK_DATA.breakingSummary as any} />
        </ViewShell>
      );
    case 'policy':
      return (
        <ViewShell title={def.title} subtitle={def.subtitle}>
          <PolicyView data={MOCK_DATA.policySummary as any} />
        </ViewShell>
      );
    case 'graph':
      return (
        <ViewShell title={def.title} subtitle={def.subtitle}>
          <GraphView graphData={MOCK_DATA.graph as any} cascades={MOCK_DATA.cascades as any} />
        </ViewShell>
      );
    case 'notifications':
      return (
        <ViewShell title={def.title} subtitle={def.subtitle}>
          <NotificationsView notifications={MOCK_DATA.notifications as any} channels={MOCK_DATA.channels as any} />
        </ViewShell>
      );
    default:
      return (
        <ViewShell title={def.title} subtitle={def.subtitle}>
          <OverviewView />
        </ViewShell>
      );
  }
}

/**
 * Root page. Reads ?view= search param and renders the active view.
 */
export default function HomePage() {
  const [activeView, setActiveView] = useState<ViewId>(() => {
    if (typeof window === 'undefined') return DEFAULT_VIEW;
    const params = new URLSearchParams(window.location.search);
    const viewParam = params.get('view');
    return isValidViewId(viewParam) ? viewParam : DEFAULT_VIEW;
  });

  const handleViewChange = useCallback((viewId: ViewId) => {
    setActiveView(viewId);
    const url = new URL(window.location.href);
    url.searchParams.set('view', viewId);
    window.history.pushState({}, '', url.toString());
  }, []);

  // Use nav item label for breadcrumb (short name matching design reference)
  const navItem = NAV_ITEMS.find((item) => item.viewId === activeView);
  const breadcrumbLabel = navItem?.label ?? VIEW_REGISTRY[activeView].title;

  return (
    <AppLayout
      nav={
        <NavRail
          activeView={activeView}
          onViewChange={handleViewChange}
        />
      }
      breadcrumb={breadcrumbLabel}
      lastIngestion="3 min ago"
    >
      {renderView(activeView)}
    </AppLayout>
  );
}
