/**
 * Contract Freeze: Event Schemas
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md
 *
 * Frontend event contracts. These are in-process events (not backend domain events)
 * that the frontend uses for cross-component communication:
 * - View switching
 * - Theme changes
 * - Notification updates (polling results)
 * - Error boundary events
 */
import type { ViewId } from '@/lib/contracts/types';
import type { Theme } from '@/lib/contracts/theme';
import type { ApiError } from '@/lib/contracts/errors';

// ──────────────────────────────────────────────
// View Events
// ──────────────────────────────────────────────

/** Fired when the user switches to a different view. */
export interface ViewChangedEvent {
  type: 'view.changed';
  payload: {
    previousView: ViewId;
    currentView: ViewId;
    timestamp: string;
  };
}

// ──────────────────────────────────────────────
// Theme Events
// ──────────────────────────────────────────────

/** Fired when the theme is toggled. */
export interface ThemeChangedEvent {
  type: 'theme.changed';
  payload: {
    previousTheme: Theme;
    currentTheme: Theme;
    timestamp: string;
  };
}

// ──────────────────────────────────────────────
// Data Events
// ──────────────────────────────────────────────

/** Fired when a view's data fetch succeeds. */
export interface DataFetchedEvent {
  type: 'data.fetched';
  payload: {
    viewId: ViewId;
    endpointKey: string;
    durationMs: number;
    timestamp: string;
  };
}

/** Fired when a view's data fetch fails. */
export interface DataFetchFailedEvent {
  type: 'data.fetch-failed';
  payload: {
    viewId: ViewId;
    endpointKey: string;
    error: ApiError;
    timestamp: string;
  };
}

// ──────────────────────────────────────────────
// Notification Events
// ──────────────────────────────────────────────

/** Fired when notification polling returns new data. */
export interface NotificationUpdateEvent {
  type: 'notification.update';
  payload: {
    unreadCount: number;
    totalCount: number;
    timestamp: string;
  };
}

// ──────────────────────────────────────────────
// Error Boundary Events
// ──────────────────────────────────────────────

/** Fired when an error boundary catches an error. */
export interface ErrorCaughtEvent {
  type: 'error.caught';
  payload: {
    viewId: ViewId;
    error: ApiError;
    timestamp: string;
  };
}

/** Fired when the user retries after an error. */
export interface RetryEvent {
  type: 'error.retry';
  payload: {
    viewId: ViewId;
    endpointKey: string;
    timestamp: string;
  };
}

// ──────────────────────────────────────────────
// Union Type
// ──────────────────────────────────────────────

/** All frontend events. */
export type FrontendEvent =
  | ViewChangedEvent
  | ThemeChangedEvent
  | DataFetchedEvent
  | DataFetchFailedEvent
  | NotificationUpdateEvent
  | ErrorCaughtEvent
  | RetryEvent;

/** Event handler type. */
export type EventHandler<E extends FrontendEvent = FrontendEvent> = (event: E) => void;

/** Event bus interface for in-process frontend events. */
export interface EventBus {
  /** Subscribe to a specific event type. */
  on<E extends FrontendEvent>(type: E['type'], handler: EventHandler<E>): () => void;

  /** Unsubscribe from a specific event type. */
  off<E extends FrontendEvent>(type: E['type'], handler: EventHandler<E>): void;

  /** Emit an event to all subscribers. */
  emit<E extends FrontendEvent>(event: E): void;

  /** Clear all subscribers (for cleanup). */
  clear(): void;
}
