'use client';

import { useState, useCallback } from 'react';
import { AppLayout } from '@/components/layout/AppLayout';
import { NavRail } from '@/components/layout/NavRail';
import { isValidViewId, DEFAULT_VIEW, VIEW_REGISTRY } from '@/lib/contracts/view-config';
import type { ViewId } from '@/lib/contracts/types';
import { OverviewView } from '@/components/overview/OverviewView';
import { InventoryView } from '@/components/inventory/InventoryView';
import { BreakingView } from '@/components/breaking/BreakingView';
import { PolicyView } from '@/components/policy/PolicyView';
import { GraphView } from '@/components/graph/GraphView';
import { NotificationsView } from '@/components/notifications/NotificationsView';

/**
 * Mock data for development — will be replaced with API data fetching.
 * @see lib/contracts/api-client.ts for the ViewDataService contract.
 */
const MOCK_DATA = {
  overview: {
    overallScore: 88,
    dimensions: {
      compliance: { value: 94, pct: 94, label: '94% pass rate', trend: 'up' as const, tone: 'success' as const },
      breaking: { value: 7, pct: 30, label: '7 open breakages', trend: 'down' as const, tone: 'warn' as const },
      coverage: { value: 85, pct: 85, label: '85% APIs covered', trend: 'stable' as const, tone: 'accent' as const },
      staleness: { value: 3, pct: 15, label: '3 stale APIs', trend: 'up' as const, tone: 'neutral' as const },
      impact: { value: 92, pct: 92, label: '92% mapped', trend: 'up' as const, tone: 'success' as const },
    },
    summary: {
      totalApis: 24,
      activePolicies: 18,
      breakingChanges30d: 7,
      servicesAtRisk: 3,
      dependencyEdges: 45,
    },
    recentBreakages: [
      { serviceName: 'payment-svc', changeType: 'field-removal', severity: 'critical' as const, relativeTime: '2h ago' },
      { serviceName: 'checkout-api', changeType: 'type-change', severity: 'high' as const, relativeTime: '5h ago' },
    ],
    topViolations: [
      { serviceName: 'user-svc', policyName: 'no-breaking-changes', violationCount: 4, trend: 2 },
      { serviceName: 'inventory-svc', policyName: 'naming-convention', violationCount: 3, trend: -1 },
    ],
  },
  apis: [
    { id: '1', serviceName: 'payment-svc', version: 'v2.1.0', specFormat: 'OpenAPI 3.1' as const, health: 'healthy' as const, lastAnalyzed: '2026-06-13', owner: 'payments' },
    { id: '2', serviceName: 'user-svc', version: 'v3.0.0', specFormat: 'OpenAPI 3.0' as const, health: 'at-risk' as const, lastAnalyzed: '2026-06-10', owner: 'identity', policyPassRate: 65, openBreakages: 4 },
    { id: '3', serviceName: 'checkout-api', version: 'v1.5.0', specFormat: 'OpenAPI 3.1' as const, health: 'warning' as const, lastAnalyzed: '2026-06-08', owner: 'commerce', policyPassRate: 78 },
  ],
  staleApis: [
    { id: '4', serviceName: 'legacy-catalog', lastIngested: '2026-04-01', daysStale: 73, version: 'v0.9.0' },
    { id: '5', serviceName: 'reporting-svc', lastIngested: '2026-05-15', daysStale: 29, version: 'v1.0.0' },
  ],
  breakingSummary: {
    total30d: 7,
    critical: 2,
    high: 3,
    nonBreaking: 2,
    items: [
      { id: 'b1', serviceName: 'payment-svc', changeType: 'field-removal' as const, severity: 'critical' as const, detectedAt: '2026-06-13T10:30:00Z', versionFrom: 'v2.0.0', versionTo: 'v2.1.0', diffText: '-  /components/schemas/PaymentRequest\n-    properties:\n-      cardNumber:\n+  missing required field', impactedConsumers: ['checkout-api', 'admin-panel'] },
      { id: 'b2', serviceName: 'user-svc', changeType: 'type-change' as const, severity: 'high' as const, detectedAt: '2026-06-12T14:00:00Z', versionFrom: 'v2.9.0', versionTo: 'v3.0.0', diffText: '-  type: integer\n+  type: string', impactedConsumers: ['profile-svc'] },
    ],
  },
  policySummary: {
    activePolicies: 18,
    passRate: 83,
    openViolations: 12,
    coveredApis: 20,
    policies: [
      { id: 'p1', name: 'no-breaking-changes', description: 'All API changes must be backwards-compatible', scope: 'all APIs', status: 'violated' as const, violationCount: 4, violatingServices: ['payment-svc', 'user-svc'] },
      { id: 'p2', name: 'naming-convention', description: 'Endpoints must follow kebab-case naming', scope: 'user-facing APIs', status: 'violated' as const, violationCount: 3, violatingServices: ['inventory-svc'] },
      { id: 'p3', name: 'require-description', description: 'Every endpoint must have a description field', scope: 'all APIs', status: 'passing' as const, violationCount: 0, violatingServices: [] },
    ],
  },
  graph: {
    nodes: [
      { id: 'pay', label: 'payment-svc', subtitle: 'API · v2.1.0', kind: 'api' as const, x: 150, y: 100, impacted: false },
      { id: 'usr', label: 'user-svc', subtitle: 'API · v3.0.0', kind: 'api' as const, x: 400, y: 80, impacted: true },
      { id: 'chk', label: 'checkout-api', subtitle: 'API · v1.5.0', kind: 'api' as const, x: 650, y: 120, impacted: true },
      { id: 'inv', label: 'inventory-svc', subtitle: 'SVC · v1.2.0', kind: 'svc' as const, x: 300, y: 250, impacted: false },
      { id: 'prf', label: 'profile-svc', subtitle: 'SVC · v2.0.0', kind: 'svc' as const, x: 550, y: 280, impacted: true },
    ],
    edges: [
      { from: 'pay', to: 'chk', impacted: true },
      { from: 'pay', to: 'usr', impacted: false },
      { from: 'usr', to: 'prf', impacted: true },
      { from: 'chk', to: 'inv', impacted: false },
    ],
  },
  cascades: [
    { id: 'c1', sourceService: 'payment-svc', sourceVersion: 'v2.1.0', changeDescription: 'Field removal: cardNumber in PaymentRequest', downstreamServices: ['checkout-api', 'admin-panel'], totalConsumers: 2, severity: 'critical' as const },
  ],
  notifications: [
    { id: 'n1', title: 'Breaking change detected in payment-svc', description: 'Field removal detected in PaymentRequest schema', severity: 'critical' as const, channel: 'slack' as const, channelDetail: '#api-gov', read: false, timestamp: '2026-06-13T10:30:00Z', relativeTime: '2 hours ago' },
    { id: 'n2', title: 'Policy violation: payment-svc', description: '3 new violations of no-breaking-changes policy', severity: 'high' as const, channel: 'email' as const, channelDetail: 'gov-team@company.com', read: false, timestamp: '2026-06-13T10:35:00Z', relativeTime: '2 hours ago' },
    { id: 'n3', title: 'User-svc re-evaluated', description: 'Policy evaluation passed for latest ingestion', severity: 'warning' as const, channel: 'email' as const, channelDetail: 'gov-team@company.com', read: true, timestamp: '2026-06-12T09:00:00Z', relativeTime: '1 day ago' },
  ],
  channels: [
    { id: 'ch1', type: 'slack' as const, status: 'active' as const, config: { target: '#api-gov', rules: ['on-breaking-change', 'on-policy-violation'], lastDelivered: '2 hours ago' } },
    { id: 'ch2', type: 'email' as const, status: 'active' as const, config: { target: 'gov-team@company.com', rules: ['on-breaking-change'], lastDelivered: '2 hours ago' } },
  ],
};

function renderView(viewId: ViewId) {
  switch (viewId) {
    case 'overview':
      return <OverviewView data={MOCK_DATA.overview as any} />;
    case 'inventory':
      return <InventoryView apis={MOCK_DATA.apis as any} staleApis={MOCK_DATA.staleApis as any} />;
    case 'breaking':
      return <BreakingView data={MOCK_DATA.breakingSummary as any} />;
    case 'policy':
      return <PolicyView data={MOCK_DATA.policySummary as any} />;
    case 'graph':
      return <GraphView graphData={MOCK_DATA.graph as any} cascades={MOCK_DATA.cascades as any} />;
    case 'notifications':
      return <NotificationsView notifications={MOCK_DATA.notifications as any} channels={MOCK_DATA.channels as any} />;
    default:
      return <OverviewView />;
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

  const viewDef = VIEW_REGISTRY[activeView];

  return (
    <AppLayout
      nav={
        <NavRail
          activeView={activeView}
          onViewChange={handleViewChange}
        />
      }
      breadcrumb={viewDef.title}
    >
      {renderView(activeView)}
    </AppLayout>
  );
}
