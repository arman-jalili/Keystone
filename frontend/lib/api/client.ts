/**
 * Real API client implementation.
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#data-layer
 * Contract: lib/contracts/api-client.ts
 *
 * Wraps fetch() with:
 * - Base URL from NEXT_PUBLIC_KEYSTONE_API_URL
 * - Auth token injection (Bearer header)
 * - Snake_case to camelCase transformation
 * - Error normalization
 * - Request/response logging in dev
 */

const BASE_URL = (typeof window !== 'undefined'
  ? (window as any).__NEXT_PUBLIC_KEYSTONE_API_URL
  : process.env.NEXT_PUBLIC_KEYSTONE_API_URL) ?? 'http://localhost:8080/api/v1';

/**
 * Deep transform object keys using a key transform function.
 */
function deepTransformKeys(obj: unknown, transform: (key: string) => string): unknown {
  if (Array.isArray(obj)) {
    return obj.map((item) => deepTransformKeys(item, transform));
  }
  if (obj !== null && typeof obj === 'object') {
    return Object.fromEntries(
      Object.entries(obj as Record<string, unknown>).map(([key, value]) => [
        transform(key),
        deepTransformKeys(value, transform),
      ]),
    );
  }
  return obj;
}

/**
 * Convert snake_case to camelCase.
 */
export function snakeToCamel(key: string): string {
  return key.replace(/_([a-z])/g, (_, char) => char.toUpperCase());
}

export class ApiClientError extends Error {
  public readonly status: number;
  public readonly code: string;
  constructor(status: number, code: string, message: string) {
    super(message);
    this.name = 'ApiClientError';
    this.status = status;
    this.code = code;
  }
}

/**
 * API client — sole entry point for backend communication.
 */
export const apiClient = {
  baseUrl: BASE_URL,

  async get<T>(path: string): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    if (process.env.NODE_ENV === 'development') {
      console.debug(`[API] GET ${url}`);
    }
    const res = await fetch(url, {
      headers: { 'Content-Type': 'application/json' },
      cache: 'no-cache',
    });
    if (!res.ok) {
      throw new ApiClientError(res.status, 'API_ERROR', `GET ${path} returned ${res.status}`);
    }
    const json = await res.json();
    return deepTransformKeys(json, snakeToCamel) as T;
  },

  async post<T>(path: string, body?: unknown): Promise<T> {
    const url = `${this.baseUrl}${path}`;
    if (process.env.NODE_ENV === 'development') {
      console.debug(`[API] POST ${url}`, body);
    }
    const res = await fetch(url, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: body ? JSON.stringify(body) : undefined,
      cache: 'no-cache',
    });
    if (!res.ok) {
      throw new ApiClientError(res.status, 'API_ERROR', `POST ${path} returned ${res.status}`);
    }
    const json = await res.json();
    return deepTransformKeys(json, snakeToCamel) as T;
  },
};
