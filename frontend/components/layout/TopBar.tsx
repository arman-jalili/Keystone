interface TopBarProps {
  /** Current view label for breadcrumb display */
  breadcrumb: string;
  /** Relative time of last ingestion (e.g. "3 min ago") */
  lastIngestion?: string;
}

/**
 * 56px top bar with breadcrumb (left) and live indicator (right).
 * Renders as a server component by default; client wrapper if polling needed.
 */
export function TopBar({ breadcrumb, lastIngestion }: TopBarProps) {
  return (
    <header className="flex h-14 shrink-0 items-center justify-between border-b border-border bg-surface px-10">
      {/* Breadcrumb */}
      <nav aria-label="Breadcrumb" className="font-mono text-[11px] uppercase tracking-[0.06em] text-muted">
        <span>Keystone</span>
        <span className="mx-2">/</span>
        <span className="font-medium text-fg">{breadcrumb}</span>
      </nav>

      {/* Live indicator */}
      <div className="flex items-center gap-2">
        <span className="relative flex h-1.5 w-1.5">
          <span className="absolute inline-flex h-full w-full animate-ping rounded-full bg-success opacity-75" />
          <span className="relative inline-flex h-1.5 w-1.5 rounded-full bg-success" />
        </span>
        {lastIngestion ? (
          <span className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
            Last ingestion {lastIngestion}
          </span>
        ) : (
          <span className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
            Live
          </span>
        )}
      </div>
    </header>
  );
}
