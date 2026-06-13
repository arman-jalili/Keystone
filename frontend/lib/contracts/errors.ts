/**
 * Contract Freeze: Error Types
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#error-handling
 * Source: design/data-schema.md
 *
 * Error response format from the backend, plus frontend error wrappers.
 */
import type { AsyncData } from './types';

/**
 * Backend error response (standard format).
 */
export interface BackendErrorResponse {
  error: string;
  message: string;
  status: number;
  timestamp?: string;
  path?: string;
  details?: Record<string, string[]>;
}

/**
 * Frontend-normalized API error.
 */
export class ApiError extends Error {
  public readonly status: number;
  public readonly code: string;
  public readonly details?: Record<string, string[]>;

  constructor(status: number, code: string, message: string, details?: Record<string, string[]>) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.details = details;
  }

  /** 4xx client errors */
  get isClientError(): boolean {
    return this.status >= 400 && this.status < 500;
  }

  /** 5xx server errors */
  get isServerError(): boolean {
    return this.status >= 500;
  }

  /** 401 Unauthorized */
  get isUnauthorized(): boolean {
    return this.status === 401;
  }

  /** 404 Not Found */
  get isNotFound(): boolean {
    return this.status === 404;
  }

  /** 422 Unprocessable Entity */
  get isUnprocessable(): boolean {
    return this.status === 422;
  }

  /** 429 Rate Limited */
  get isRateLimited(): boolean {
    return this.status === 429;
  }
}

/**
 * Network-level error (fetch failed, timeout, etc.).
 */
export class NetworkError extends Error {
  public readonly cause: unknown;

  constructor(message: string, cause?: unknown) {
    super(message);
    this.name = 'NetworkError';
    this.cause = cause;
  }
}

/**
 * Error when a view has no data yet (not an error, just empty).
 */
export class EmptyDataError extends Error {
  public readonly viewId: string;

  constructor(viewId: string, message?: string) {
    super(message ?? `No data available for view: ${viewId}`);
    this.name = 'EmptyDataError';
    this.viewId = viewId;
  }
}

/**
 * Error normalizer — converts raw fetch errors to typed errors.
 */
export interface ErrorNormalizer {
  /**
   * Normalize a fetch Response error into a typed ApiError.
   * Handles: non-2xx status codes, malformed JSON, network failures.
   */
  normalizeResponseError(response: Response, body?: string): ApiError;

  /**
   * Normalize a fetch-level failure (network error, timeout, abort).
   */
  normalizeNetworkError(error: unknown): NetworkError;

  /**
   * Check if a response body represents an empty dataset.
   */
  isEmptyResponse(response: Response, data: unknown): boolean;
}

 /**
 * Async operation state — covers loading, success, error, and empty.
 */
export type AsyncData<T> =
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; error: ApiError }
  | { status: 'empty' };
