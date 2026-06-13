import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { OverviewView } from '@/components/overview/OverviewView';
import { BreakingView } from '@/components/breaking/BreakingView';
import { PolicyView } from '@/components/policy/PolicyView';
import { InventoryView } from '@/components/inventory/InventoryView';
import { NotificationsView } from '@/components/notifications/NotificationsView';

const mockGovernanceHealth = {
  overallScore: 88,
  dimensions: {
    compliance: { value: 94, pct: 94, label: '94% pass rate', trend: 'up' as const, tone: 'success' as const },
    breaking: { value: 7, pct: 30, label: '7 open breakages', trend: 'down' as const, tone: 'warn' as const },
    coverage: { value: 85, pct: 85, label: '85% APIs covered', trend: 'stable' as const, tone: 'accent' as const },
    staleness: { value: 3, pct: 15, label: '3 stale APIs', trend: 'up' as const, tone: 'neutral' as const },
    impact: { value: 92, pct: 92, label: '92% mapped', trend: 'up' as const, tone: 'success' as const },
  },
  summary: { totalApis: 24, activePolicies: 18, breakingChanges30d: 7, servicesAtRisk: 3, dependencyEdges: 45 },
  recentBreakages: [
    { serviceName: 'payment-svc', changeType: 'field-removal', severity: 'critical' as const, relativeTime: '2h ago' },
  ],
  topViolations: [
    { serviceName: 'user-svc', policyName: 'no-breaking-changes', violationCount: 4, trend: 2 },
  ],
};

const mockBreakingSummary = {
  total30d: 7,
  critical: 2,
  high: 3,
  nonBreaking: 2,
  items: [
    {
      id: 'b1', serviceName: 'payment-svc', changeType: 'field-removal' as const, severity: 'critical' as const,
      detectedAt: '2026-06-13', versionFrom: 'v2.0.0', versionTo: 'v2.1.0',
      diffText: '-  cardNumber\n+  newField', impactedConsumers: ['checkout-api'],
    },
  ],
};

const mockPolicySummary = {
  activePolicies: 18, passRate: 83, openViolations: 12, coveredApis: 20,
  policies: [
    { id: 'p1', name: 'no-breaking-changes', description: 'Backwards-compatible', scope: 'all APIs', status: 'violated' as const, violationCount: 4, violatingServices: ['payment-svc'] },
    { id: 'p2', name: 'require-description', description: 'Description required', scope: 'all APIs', status: 'passing' as const, violationCount: 0, violatingServices: [] },
  ],
};

describe('OverviewView', () => {
  it('renders score and stats', () => {
    render(<OverviewView data={mockGovernanceHealth as any} />);
    expect(screen.getByText('Total APIs')).toBeDefined();
    expect(screen.getByText('24')).toBeDefined();
    expect(screen.getByText('Active Policies')).toBeDefined();
    expect(screen.getByText('18')).toBeDefined();
  });

  it('renders recent breakages table', () => {
    render(<OverviewView data={mockGovernanceHealth as any} />);
    expect(screen.getByText('payment-svc')).toBeDefined();
    expect(screen.getByText('Recent Breakages')).toBeDefined();
  });

  it('shows empty state when no data', () => {
    render(<OverviewView />);
    expect(screen.getByText('No governance data available.')).toBeDefined();
  });
});

describe('BreakingView', () => {
  it('renders stats and breakage cards', () => {
    render(<BreakingView data={mockBreakingSummary as any} />);
    expect(screen.getByText('Total (30d)')).toBeDefined();
    expect(screen.getByText('7')).toBeDefined();
    expect(screen.getByText('payment-svc')).toBeDefined();
  });

  it('shows empty state when no data', () => {
    render(<BreakingView />);
    expect(screen.getByText('No breaking changes detected.')).toBeDefined();
  });
});

describe('PolicyView', () => {
  it('renders stats and rule cards', () => {
    render(<PolicyView data={mockPolicySummary as any} />);
    expect(screen.getByText('Active Policies')).toBeDefined();
    expect(screen.getByText('18')).toBeDefined();
    expect(screen.getByText('no-breaking-changes')).toBeDefined();
    expect(screen.getByText('require-description')).toBeDefined();
  });

  it('shows empty state when no data', () => {
    render(<PolicyView />);
    expect(screen.getByText('No policies defined.')).toBeDefined();
  });
});

describe('InventoryView', () => {
  it('renders API table', () => {
    const apis = [
      { id: '1', serviceName: 'payment-svc', version: 'v2.1.0', specFormat: 'OpenAPI 3.1' as const, health: 'healthy' as const, lastAnalyzed: '2026-06-13', owner: 'payments' },
    ];
    render(<InventoryView apis={apis as any} />);
    expect(screen.getByText('payment-svc')).toBeDefined();
    expect(screen.getByText('All Specifications')).toBeDefined();
  });

  it('renders stale APIs section', () => {
    const staleApis = [
      { id: '4', serviceName: 'legacy-catalog', lastIngested: '2026-04-01', daysStale: 73, version: 'v0.9.0' },
    ];
    render(<InventoryView staleApis={staleApis as any} />);
    expect(screen.getByText('Stale Specifications')).toBeDefined();
    expect(screen.getByText('legacy-catalog')).toBeDefined();
  });
});

describe('NotificationsView', () => {
  const notifications = [
    { id: 'n1', title: 'Breaking change detected', description: 'Field removal', severity: 'critical' as const, channel: 'slack' as const, channelDetail: '#api-gov', read: false, timestamp: '2026-06-13T10:30:00Z', relativeTime: '2 hours ago' },
    { id: 'n2', title: 'Old notification', description: 'Resolved', severity: 'warning' as const, channel: 'email' as const, channelDetail: 'gov@co', read: true, timestamp: '2026-06-12T09:00:00Z', relativeTime: '1 day ago' },
  ];

  const channels = [
    { id: 'ch1', type: 'slack' as const, status: 'active' as const, config: { target: '#api-gov', rules: ['on-breaking-change'], lastDelivered: '2 hours ago' } },
  ];

  it('renders unread and read sections', () => {
    render(<NotificationsView notifications={notifications as any} channels={channels as any} />);
    expect(screen.getByText('Unread')).toBeDefined();
    expect(screen.getByText('Read')).toBeDefined();
    expect(screen.getByText('Breaking change detected')).toBeDefined();
    expect(screen.getByText('Old notification')).toBeDefined();
  });

  it('renders channel cards', () => {
    render(<NotificationsView notifications={notifications as any} channels={channels as any} />);
    expect(screen.getByText('Channels')).toBeDefined();
    expect(screen.getByText('Slack')).toBeDefined();
  });

  it('shows stat grid with counts', () => {
    render(<NotificationsView notifications={notifications as any} channels={channels as any} />);
    expect(screen.getByText('Total (7d)')).toBeDefined();
    expect(screen.getByText('2')).toBeDefined();
    expect(screen.getByText('Unread')).toBeDefined();
    expect(screen.getByText('1')).toBeDefined();
  });
});
