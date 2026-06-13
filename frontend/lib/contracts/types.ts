/**
 * Contract Freeze: Data Transfer Objects
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md
 * Source: design/data-schema.md
 *
 * All DTOs matching the Keystone Backend REST API responses.
 * These are the frozen contracts that the frontend depends on.
 * DO NOT modify without updating the backend API contract.
 */

// ──────────────────────────────────────────────
// GovernanceHealth — Overview view
// ──────────────────────────────────────────────

export interface GovernanceHealth {
  overallScore: number;
  dimensions: {
    compliance: HealthDimension;
    breaking: HealthDimension;
    coverage: HealthDimension;
    staleness: HealthDimension;
    impact: HealthDimension;
  };
  summary: {
    totalApis: number;
    activePolicies: number;
    breakingChanges30d: number;
    servicesAtRisk: number;
    dependencyEdges: number;
  };
  recentBreakages: RecentBreakage[];
  topViolations: TopViolation[];
}

export interface HealthDimension {
  value: number;
  pct: number;
  label: string;
  trend?: 'up' | 'down' | 'stable';
  tone: 'success' | 'warn' | 'danger' | 'accent' | 'neutral';
}

export interface RecentBreakage {
  serviceName: string;
  changeType: 'field-removal' | 'type-change' | 'path-removal' | 'enum-change';
  severity: 'critical' | 'high';
  relativeTime: string;
}

export interface TopViolation {
  serviceName: string;
  policyName: string;
  violationCount: number;
  trend: number;
}

// ──────────────────────────────────────────────
// API Inventory — Inventory view
// ──────────────────────────────────────────────

export interface ApiInventoryItem {
  id: string;
  serviceName: string;
  version: string;
  specFormat: 'OpenAPI 3.0' | 'OpenAPI 3.1';
  health: 'healthy' | 'low-risk' | 'warning' | 'at-risk';
  lastAnalyzed: string;
  owner: string;
  policyPassRate?: number;
  openBreakages?: number;
}

export interface StaleApiItem {
  id: string;
  serviceName: string;
  lastIngested: string;
  daysStale: number;
  version: string;
}

// ──────────────────────────────────────────────
// Breaking Changes — Breaking view
// ──────────────────────────────────────────────

export interface BreakingChange {
  id: string;
  serviceName: string;
  changeType: 'field-removal' | 'type-change' | 'path-removal' | 'enum-change';
  severity: 'critical' | 'high';
  detectedAt: string;
  versionFrom: string;
  versionTo: string;
  diffText: string;
  impactedConsumers: string[];
}

export interface BreakingChangeSummary {
  total30d: number;
  critical: number;
  high: number;
  nonBreaking: number;
  items: BreakingChange[];
}

// ──────────────────────────────────────────────
// Policies — Policy view
// ──────────────────────────────────────────────

export interface Policy {
  id: string;
  name: string;
  description: string;
  scope: string;
  status: 'passing' | 'violated';
  violationCount: number;
  violatingServices: string[];
}

export interface PolicySummary {
  activePolicies: number;
  passRate: number;
  openViolations: number;
  coveredApis: number;
  policies: Policy[];
}

// ──────────────────────────────────────────────
// Dependency Graph — Graph view
// ──────────────────────────────────────────────

export interface DependencyGraphData {
  nodes: GraphNode[];
  edges: GraphEdge[];
}

export interface GraphNode {
  id: string;
  label: string;
  subtitle: string;
  kind: 'api' | 'svc' | 'ui';
  x: number;
  y: number;
  impacted: boolean;
}

export interface GraphEdge {
  from: string;
  to: string;
  impacted: boolean;
}

export interface ImpactCascade {
  id: string;
  sourceService: string;
  sourceVersion: string;
  changeDescription: string;
  downstreamServices: string[];
  totalConsumers: number;
  severity: 'critical' | 'high';
}

// ──────────────────────────────────────────────
// Notifications — Notifications view
// ──────────────────────────────────────────────

export interface Notification {
  id: string;
  title: string;
  description: string;
  severity: 'critical' | 'high' | 'warning';
  channel: 'slack' | 'email' | 'webhook';
  channelDetail: string;
  read: boolean;
  timestamp: string;
  relativeTime: string;
}

export interface NotificationChannel {
  id: string;
  type: 'slack' | 'email' | 'webhook';
  status: 'active' | 'inactive';
  config: {
    target: string;
    rules: string[];
    lastDelivered: string;
  };
}

export interface NotificationSummary {
  total7d: number;
  unread: number;
  activeChannels: number;
  deliveryRate: number;
}

// ──────────────────────────────────────────────
// Utility Types
// ──────────────────────────────────────────────

export type StatTone = 'accent' | 'success' | 'danger' | 'warn';

export type PillTone = 'critical' | 'high' | 'low' | 'info' | 'pass' | 'fail' | 'warn';

export type ViewId = 'overview' | 'inventory' | 'breaking' | 'policy' | 'graph' | 'notifications';

export type ChangeType = 'field-removal' | 'type-change' | 'path-removal' | 'enum-change';

export type Severity = 'critical' | 'high' | 'warning';

export type HealthStatus = 'healthy' | 'low-risk' | 'warning' | 'at-risk';

export type PolicyStatus = 'passing' | 'violated';

export type ChannelType = 'slack' | 'email' | 'webhook';

export type ChannelStatus = 'active' | 'inactive';

export type TrendDirection = 'up' | 'down' | 'stable';

// ──────────────────────────────────────────────
// Async Data State
// ──────────────────────────────────────────────

/**
 * Discriminated union for async data loading states.
 * Every data-dependent view uses this pattern.
 *
 * @see ApiError in lib/contracts/errors.ts for the error type.
 */
export type AsyncData<T> =
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: import('./errors').ApiError }
  | { status: 'empty' };

