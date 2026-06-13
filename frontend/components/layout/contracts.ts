/**
 * Contract Freeze: Layout Component Props
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#components
 * Source: design/components.md
 */

import type { ReactNode } from 'react';
import type { NavItem, ViewId } from '@/lib/contracts/view-config';

// ──────────────────────────────────────────────
// AppLayout
// ──────────────────────────────────────────────

export interface AppLayoutProps {
  /** Main content rendered inside the layout */
  children: ReactNode;
}

// ──────────────────────────────────────────────
// NavRail
// ──────────────────────────────────────────────

export interface NavRailProps {
  /** Current active view */
  activeView: ViewId;
  /** Callback when a nav item is clicked */
  onViewChange: (viewId: ViewId) => void;
  /** Current theme for the toggle display */
  currentTheme: 'light' | 'dark';
  /** Callback when theme is toggled */
  onThemeToggle: () => void;
  /** Badge values per view (overrides defaults) */
  badges?: Partial<Record<ViewId, number | undefined>>;
}

export interface NavItemProps {
  /** Nav item definition */
  item: NavItem;
  /** Whether this item is currently active */
  isActive: boolean;
  /** Click handler */
  onClick: () => void;
  /** Current badge value (overrides item default) */
  badge?: number;
}

// ──────────────────────────────────────────────
// TopBar
// ──────────────────────────────────────────────

export interface TopBarProps {
  /** Current view label for breadcrumb display */
  breadcrumb: string;
  /** Relative time of last ingestion (e.g. "3 min ago") */
  lastIngestion?: string;
}

// ──────────────────────────────────────────────
// ThemeToggle
// ──────────────────────────────────────────────

export interface ThemeToggleProps {
  /** Current theme */
  currentTheme: 'light' | 'dark';
  /** Toggle handler */
  onToggle: () => void;
}
