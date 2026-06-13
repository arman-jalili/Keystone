import type { ReactNode } from 'react';
import { TopBar } from './TopBar';

interface AppLayoutProps {
  /** Navigation rail (NavRail component) */
  nav: ReactNode;
  /** Main content area */
  children: ReactNode;
  /** Breadcrumb text for TopBar */
  breadcrumb?: string;
  /** Last ingestion time for TopBar */
  lastIngestion?: string;
}

/**
 * Root layout shell.
 * Flex row: NavRail (fixed 232px) + main content area.
 */
export function AppLayout({ nav, children, breadcrumb, lastIngestion }: AppLayoutProps) {
  return (
    <div className="flex h-full w-full">
      {/* Left nav rail */}
      {nav}

      {/* Main content area */}
      <div className="flex flex-1 flex-col overflow-hidden">
        <TopBar breadcrumb={breadcrumb ?? ''} lastIngestion={lastIngestion} />
        <main className="flex-1 overflow-y-auto" style={{ padding: 'var(--content-padding, 32px 40px 48px)' }}>
          {children}
        </main>
      </div>
    </div>
  );
}
