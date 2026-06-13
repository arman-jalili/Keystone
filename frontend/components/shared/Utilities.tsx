import type { ReactNode } from 'react';

// ──────────────────────────────────────────────
// SectionLabel
// ──────────────────────────────────────────────

interface SectionLabelProps {
  children: string;
}

/**
 * Mono uppercase section header.
 * Used to separate sections within a view (e.g. "Stale Specifications", "Critical Breakages").
 */
export function SectionLabel({ children }: SectionLabelProps) {
  return (
    <h2 className="font-mono text-[10px] font-medium uppercase tracking-[0.08em] text-fg">
      {children}
    </h2>
  );
}

// ──────────────────────────────────────────────
// TwoCol
// ──────────────────────────────────────────────

interface TwoColProps {
  left: ReactNode;
  right: ReactNode;
}

/**
 * Two-column grid layout (1fr 1fr, 24px gap).
 */
export function TwoCol({ left, right }: TwoColProps) {
  return (
    <div className="grid grid-cols-2 gap-6">
      <div>{left}</div>
      <div>{right}</div>
    </div>
  );
}

// ──────────────────────────────────────────────
// ViewSkeleton
// ──────────────────────────────────────────────

interface ViewSkeletonProps {
  viewId: string;
}

/**
 * Loading skeleton matching the view layout.
 * Uses muted background fills with animate-pulse.
 */
export function ViewSkeleton({ viewId }: ViewSkeletonProps) {
  return (
    <div className="flex flex-col gap-6 animate-pulse" aria-label={`Loading ${viewId} view`}>
      {/* Title skeleton */}
      <div className="h-8 w-1/3 rounded-none bg-border" />
      <div className="h-4 w-1/2 rounded-none bg-border" />

      {/* Stat grid skeleton (5 cells) */}
      {viewId === 'overview' && (
        <div className="grid grid-cols-5 gap-px border border-border">
          {Array.from({ length: 5 }).map((_, i) => (
            <div key={i} className="bg-surface p-6">
              <div className="mb-2 h-8 w-16 rounded-none bg-border/50" />
              <div className="h-3 w-20 rounded-none bg-border/30" />
            </div>
          ))}
        </div>
      )}

      {/* Generic skeleton for other views */}
      {viewId !== 'overview' && (
        <div className="flex flex-col gap-4">
          <div className="h-24 rounded-none border border-border bg-surface" />
          <div className="h-24 rounded-none border border-border bg-surface" />
          <div className="h-24 rounded-none border border-border bg-surface" />
        </div>
      )}
    </div>
  );
}

// ──────────────────────────────────────────────
// ErrorState
// ──────────────────────────────────────────────

interface ErrorStateProps {
  title?: string;
  message: string;
  onRetry?: () => void;
}

/**
 * Consistent error display with border-left accent line.
 * Quiet, informative, muted colors — no alarm banners.
 */
export function ErrorState({ title, message, onRetry }: ErrorStateProps) {
  return (
    <div className="border-l-3 border-accent bg-surface px-6 py-5">
      <p className="font-mono text-[10px] uppercase tracking-[0.08em] text-muted">
        {title ?? 'Unable to load view'}
      </p>
      <p className="mt-2 text-body text-fg">{message}</p>
      {onRetry && (
        <button
          type="button"
          onClick={onRetry}
          className="mt-3 font-mono text-[10px] uppercase tracking-[0.06em] text-accent underline decoration-accent/30 underline-offset-2 hover:decoration-accent"
        >
          Retry
        </button>
      )}
    </div>
  );
}

// ──────────────────────────────────────────────
// ZeroState
// ──────────────────────────────────────────────

interface ZeroStateProps {
  label: string;
  description: string;
  action?: {
    label: string;
    href?: string;
    onClick?: () => void;
  };
}

/**
 * Empty state with guidance text.
 * Shown when data is valid but empty (no violations, no policies, etc.).
 */
export function ZeroState({ label, description, action }: ZeroStateProps) {
  return (
    <div className="border border-border bg-surface px-8 py-12 text-center">
      <p className="font-mono text-[10px] uppercase tracking-[0.08em] text-muted">
        {label}
      </p>
      <p className="mx-auto mt-3 max-w-md text-body text-fg">
        {description}
      </p>
      {action && (
        <>
          {action.href ? (
            <a
              href={action.href}
              className="mt-4 inline-block font-mono text-[10px] uppercase tracking-[0.06em] text-accent underline decoration-accent/30 underline-offset-2 hover:decoration-accent"
            >
              {action.label}
            </a>
          ) : (
            <button
              type="button"
              onClick={action.onClick}
              className="mt-4 font-mono text-[10px] uppercase tracking-[0.06em] text-accent underline decoration-accent/30 underline-offset-2 hover:decoration-accent"
            >
              {action.label}
            </button>
          )}
        </>
      )}
    </div>
  );
}
