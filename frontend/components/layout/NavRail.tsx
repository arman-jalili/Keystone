'use client';

import { useCallback } from 'react';
import { NAV_ITEMS, isValidViewId, DEFAULT_VIEW } from '@/lib/contracts/view-config';
import type { ViewId } from '@/lib/contracts/types';
import { ThemeToggle } from './ThemeToggle';

interface NavRailProps {
  /** Current active view */
  activeView: ViewId;
  /** Callback when a nav item is clicked */
  onViewChange: (viewId: ViewId) => void;
  /** Badge values per view (overrides defaults) */
  badges?: Partial<Record<ViewId, number | undefined>>;
}

interface NavItemProps {
  label: string;
  viewId: ViewId;
  icon: string;
  badge?: number;
  isActive: boolean;
  onClick: () => void;
}

function NavItem({ label, viewId, icon, badge, isActive, onClick }: NavItemProps) {
  return (
    <button
      type="button"
      onClick={onClick}
      aria-current={isActive ? 'true' : undefined}
      className={`flex w-full items-center gap-3 px-3 py-2 text-left text-nav-item transition-colors ${
        isActive
          ? 'font-[510] text-accent'
          : 'text-muted hover:bg-bg hover:text-fg'
      }`}
    >
      <span className="w-6 text-center font-mono text-[10px] uppercase tracking-[0.08em]">
        {icon}
      </span>
      <span className="flex-1">{label}</span>
      {badge !== undefined && (
        <span
          className={`rounded-[2px] px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-[0.04em] ${
            isActive
              ? 'bg-accent text-white'
              : 'bg-border text-fg'
          }`}
        >
          {badge}
        </span>
      )}
    </button>
  );
}

/**
 * Fixed left navigation rail, 232px wide.
 * Reads pathname for active view and handles view switching.
 */
export function NavRail({ activeView, onViewChange, badges }: NavRailProps) {
  return (
    <nav
      className="flex h-full w-[232px] flex-col border-r border-border bg-surface"
      aria-label="Main navigation"
    >
      {/* Wordmark */}
      <div className="flex flex-col px-4 pb-6 pt-6">
        <h1 className="font-display text-[22px] font-normal leading-none text-fg">
          Keystone
        </h1>
        <p className="mt-1 font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
          API Governance
        </p>
      </div>

      {/* Nav items */}
      <div className="flex flex-1 flex-col gap-0.5 px-3">
        {NAV_ITEMS.map((item) => (
          <NavItem
            key={item.viewId}
            label={item.label}
            viewId={item.viewId}
            icon={item.icon}
            badge={badges?.[item.viewId] ?? item.badge}
            isActive={activeView === item.viewId}
            onClick={() => onViewChange(item.viewId)}
          />
        ))}
      </div>

      {/* Theme toggle at bottom */}
      <div className="flex items-center gap-3 border-t border-border px-4 py-4">
        <ThemeToggle />
        <span className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
          Theme
        </span>
      </div>
    </nav>
  );
}
