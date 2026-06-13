/**
 * Contract Freeze: API Client Interface
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#data-layer
 * Source: design/data-schema.md, design/build-instructions.md
 *
 * The HTTP client contract. All backend communication goes through this.
 * Implementation wraps fetch() with auth, error handling, and base URL.
 */
import type {
  GovernanceHealth,
  ApiInventoryItem,
  StaleApiItem,
  BreakingChangeSummary,
  PolicySummary,
  DependencyGraphData,
  ImpactCascade,
  Notification,
  NotificationChannel,
} from './types';
import type { ApiError } from './errors';

/**
 * HTTP method type for endpoint definitions.
 */
export type HttpMethod = 'GET' | 'POST' | 'PUT' | 'DELETE' | 'PATCH';

/**
 * Generic API response wrapper.
 */
export type ApiResponse<T> =
  | { ok: true; status: number; data: T }
  | { ok: false; status: number; error: ApiError };

/**
 * Options for API requests.
 */
export interface RequestOptions {
  /** Request body (for POST/PUT) */
  body?: unknown;
  /** Additional headers */
  headers?: Record<string, string>;
  /** Abort signal for cancellation */
  signal?: AbortSignal;
  /** Cache revalidation strategy */
  cache?: RequestCache;
  /** Next.js revalidation seconds */
  next?: { revalidate: number };
}

/**
 * Query parameter value types.
 */
export type QueryParamValue = string | number | boolean | undefined;

/**
 * Query parameters object.
 */
export interface QueryParams {
  [key: string]: QueryParamValue | QueryParamValue[];
}

/**
 * API Client interface - the sole contract for backend communication.
 *
 * Implementations must:
 * 1. Prefix all paths with NEXT_PUBLIC_KEYSTONE_API_URL
 * 2. Inject auth token via Bearer header
 * 3. Transform snake_case responses to camelCase
 * 4. Normalize errors into ApiError types
 * 5. Log requests/responses in development mode
 */
export interface ApiClient {
  /** Base URL for all requests (from env) */
  readonly baseUrl: string;

  /** GET request */
  get<T>(path: string, params?: QueryParams, options?: RequestOptions): Promise<ApiResponse<T>>;

  /** POST request */
  post<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>>;

  /** PUT request */
  put<T>(path: string, body?: unknown, options?: RequestOptions): Promise<ApiResponse<T>>;

  /** DELETE request */
  delete(path: string, options?: RequestOptions): Promise<ApiResponse<void>>;
}

/**
 * Fresh data fetcher per view — called by Server Components.
 * Returns raw data for server-side rendering.
 *
 * Each view has a corresponding fetch function that:
 * - Calls the correct endpoint
 * - Transforms snake_case to camelCase
 * - Returns typed data
 * - Throws on failure (caught by error boundaries)
 */
export interface ViewDataService {
  /** Overview view */
  fetchGovernanceHealth(): Promise<GovernanceHealth>;

  /** API Inventory view */
  fetchApiInventory(): Promise<ApiInventoryItem[]>;
  fetchStaleApis(): Promise<StaleApiItem[]>;

  /** Breaking Changes view */
  fetchBreakingChanges(): Promise<BreakingChangeSummary>;

  /** Policy Compliance view */
  fetchPolicies(): Promise<PolicySummary>;

  /** Dependency Graph view */
  fetchDependencyGraph(): Promise<DependencyGraphData>;
  fetchImpactCascades(): Promise<ImpactCascade[]>;

  /** Notifications view */
  fetchNotifications(): Promise<Notification[]>;
  fetchNotificationChannels(): Promise<NotificationChannel[]>;
  fetchNotificationSummary(): Promise<{ total7d: number; unread: number; activeChannels: number; deliveryRate: number }>;
}
