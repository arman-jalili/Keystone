import type { ReactNode } from 'react';

interface ViewShellProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

/**
 * Wraps each view with title + subtitle.
 * Uses display font for title, body font for subtitle.
 */
export function ViewShell({ title, subtitle, children }: ViewShellProps) {
  return (
    <div className="flex flex-col gap-6">
      <div>
        <h1 className="font-display text-view-title text-fg">{title}</h1>
        <p className="mt-1 text-body text-muted">{subtitle}</p>
      </div>
      {children}
    </div>
  );
}
