/**
 * Contract Freeze: API Endpoint Definitions
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#views-and-backend-data-sources
 * Source: design/data-schema.md
 *
 * Every endpoint the frontend calls. This is the single source of truth
 * for the API surface. Changes here require backend contract alignment.
 */
import type { HttpMethod, QueryParams } from './api-client';

/**
 * An API endpoint definition.
 */
export interface EndpointDefinition {
  /** HTTP method */
  method: HttpMethod;
  /** URL path template (e.g. "/dashboard/health") */
  path: string;
  /** Query parameters (if any) */
  params?: QueryParams;
  /** Human-readable description */
  description: string;
  /** Which view consumes this endpoint */
  view: 'overview' | 'inventory' | 'breaking' | 'policy' | 'graph' | 'notifications';
}

/**
 * All API endpoints the frontend consumes.
 * These are the frozen contracts — implementation must match exactly.
 */
export const ENDPOINTS = {
  // ── Overview ──
  DASHBOARD_HEALTH: {
    method: 'GET',
    path: '/dashboard/health',
    description: 'Aggregate governance health score and dimensions',
    view: 'overview',
  } as const satisfies EndpointDefinition,

  DASHBOARD_HEALTH_SCORE: {
    method: 'GET',
    path: '/dashboard/health-score',
    params: { period: 'LAST_30_DAYS' },
    description: 'Detailed health score with sub-scores for a time period',
    view: 'overview',
  } as const satisfies EndpointDefinition,

  // ── API Inventory ──
  INGESTION_APIS: {
    method: 'GET',
    path: '/ingestion/apis',
    description: 'List all ingested API specifications',
    view: 'inventory',
  } as const satisfies EndpointDefinition,

  INGESTION_APIS_STALE: {
    method: 'GET',
    path: '/ingestion/apis/stale',
    description: 'List stale specifications past ingestion threshold',
    view: 'inventory',
  } as const satisfies EndpointDefinition,

  // ── Breaking Changes ──
  BREAKING_REPORTS_LATEST: {
    method: 'GET',
    path: '/breaking/reports/latest',
    params: { limit: '50' },
    description: 'Latest breaking change reports across all repos',
    view: 'breaking',
  } as const satisfies EndpointDefinition,

  // ── Policy Compliance ──
  POLICIES: {
    method: 'GET',
    path: '/policies',
    description: 'List all policy rules with violation status',
    view: 'policy',
  } as const satisfies EndpointDefinition,

  POLICIES_SUMMARY: {
    method: 'GET',
    path: '/policies/summary',
    description: 'Aggregate policy compliance summary',
    view: 'policy',
  } as const satisfies EndpointDefinition,

  // ── Dependency Graph ──
  GRAPH_SERVICES: {
    method: 'GET',
    path: '/graph/services',
    description: 'List all registered services in the dependency graph',
    view: 'graph',
  } as const satisfies EndpointDefinition,

  GRAPH_IMPACT: {
    method: 'POST',
    path: '/graph/impact',
    description: 'Compute impact cascade for a breaking change',
    view: 'graph',
  } as const satisfies EndpointDefinition,

  // ── Notifications ──
  NOTIFICATIONS: {
    method: 'GET',
    path: '/notifications',
    description: 'List all notifications with delivery status',
    view: 'notifications',
  } as const satisfies EndpointDefinition,

  NOTIFICATIONS_CHANNELS: {
    method: 'GET',
    path: '/notifications/channels',
    description: 'List registered notification channels with status',
    view: 'notifications',
  } as const satisfies EndpointDefinition,
} as const;

/**
 * Resolved endpoint type — all keys of ENDPOINTS.
 */
export type EndpointKey = keyof typeof ENDPOINTS;

/**
 * Get the full URL for an endpoint (without base).
 */
export function getEndpointPath(key: EndpointKey): string {
  return ENDPOINTS[key].path;
}

/**
 * Get the HTTP method for an endpoint.
 */
export function getEndpointMethod(key: EndpointKey): HttpMethod {
  return ENDPOINTS[key].method;
}
