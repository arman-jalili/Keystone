/**
 * Contract Freeze: View Configuration
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#views-and-backend-data-sources
 * Source: design/routes.md, design/components.md
 *
 * View registry — defines all 6 views, their navigation metadata,
 * and which components they render.
 */
import type { ViewId } from './types';

/**
 * A navigation item in the left rail.
 */
export interface NavItem {
  /** View identifier (maps to ?view= param) */
  viewId: ViewId;

  /** Display label in nav */
  label: string;

  /** 2-letter mono abbreviation (e.g. "ov", "in", "br") */
  icon: string;

  /** Badge value shown in nav (number or undefined for no badge) */
  badge?: number;
}

/**
 * View metadata — what each view needs to render.
 */
export interface ViewDefinition {
  /** View identifier */
  viewId: ViewId;

  /** Display title in ViewShell */
  title: string;

  /** Subtitle in ViewShell */
  subtitle: string;

  /** Which endpoint keys this view fetches */
  endpointKeys: string[];
}

/**
 * View registry — the single source of truth for all 6 views.
 */
export const VIEW_REGISTRY: Record<ViewId, ViewDefinition> = {
  overview: {
    viewId: 'overview',
    title: 'Governance Health',
    subtitle: 'Snapshot across all registered APIs — updated on ingestion',
    endpointKeys: ['DASHBOARD_HEALTH', 'DASHBOARD_HEALTH_SCORE'],
  },
  inventory: {
    viewId: 'inventory',
    title: 'API Inventory',
    subtitle: 'All registered OpenAPI specifications — sorted by last analysis',
    endpointKeys: ['INGESTION_APIS', 'INGESTION_APIS_STALE'],
  },
  breaking: {
    viewId: 'breaking',
    title: 'Breaking Change Analysis',
    subtitle: 'Diff-based detection — field removal, type changes, path deletion',
    endpointKeys: ['BREAKING_REPORTS_LATEST'],
  },
  policy: {
    viewId: 'policy',
    title: 'Policy Compliance',
    subtitle: 'DSL-defined governance rules — scoped violation reporting per API',
    endpointKeys: ['POLICIES', 'POLICIES_SUMMARY'],
  },
  graph: {
    viewId: 'graph',
    title: 'Dependency Graph',
    subtitle: 'Service-to-service dependencies — impact propagation on change',
    endpointKeys: ['GRAPH_SERVICES', 'GRAPH_IMPACT'],
  },
  notifications: {
    viewId: 'notifications',
    title: 'Notification Center',
    subtitle: 'Multi-channel dispatch — CI status, Slack alerts, webhook events',
    endpointKeys: ['NOTIFICATIONS', 'NOTIFICATIONS_CHANNELS'],
  },
};

/**
 * Ordered list of navigation items (display order in NavRail).
 */
export const NAV_ITEMS: NavItem[] = [
  { viewId: 'overview', label: 'Overview', icon: 'ov' },
  { viewId: 'inventory', label: 'API Inventory', icon: 'in' },
  { viewId: 'breaking', label: 'Breaking Changes', icon: 'br' },
  { viewId: 'policy', label: 'Policy Compliance', icon: 'po' },
  { viewId: 'graph', label: 'Dependency Graph', icon: 'dg' },
  { viewId: 'notifications', label: 'Notifications', icon: 'nt' },
];

/**
 * Default view when no ?view= param is provided.
 */
export const DEFAULT_VIEW: ViewId = 'overview';

/**
 * Validate that a string is a valid ViewId.
 */
export function isValidViewId(value: string | null): value is ViewId {
  return value !== null && (Object.keys(VIEW_REGISTRY) as string[]).includes(value);
}
