/**
 * Contract Freeze: Shared UI Component Props
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#components
 * Source: design/components.md
 */

import type { ReactNode } from 'react';
import type { StatTone, PillTone } from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// ViewShell
// ──────────────────────────────────────────────

export interface ViewShellProps {
  /** View title (display font 32px) */
  title: string;
  /** View subtitle (body font, muted) */
  subtitle: string;
  /** View content */
  children: ReactNode;
}

// ──────────────────────────────────────────────
// StatGrid
// ──────────────────────────────────────────────

export interface StatItem {
  /** Display value (36px stat-value font) */
  value: string | number;
  /** Label (mono 10px uppercase) */
  label: string;
  /** Optional tone for color accent */
  tone?: StatTone;
}

export interface StatGridProps {
  /** Array of stat items to display */
  stats: StatItem[];
}

// ──────────────────────────────────────────────
// DataTable
// ──────────────────────────────────────────────

export interface Column {
  /** Column identifier (matches row keys) */
  key: string;
  /** Column header label (mono 10px uppercase) */
  label: string;
  /** Whether this column should use mono font for data cells */
  mono?: boolean;
  /** Whether this column contains numeric values (tabular-nums) */
  numeric?: boolean;
  /** Optional width specification */
  width?: string;
}

export interface DataTableProps {
  /** Column definitions */
  columns: Column[];
  /** Row data — each row is a record of column key → cell value */
  rows: Record<string, ReactNode>[];
  /** Optional caption below the table */
  caption?: string;
}

// ──────────────────────────────────────────────
// Pill / StatusBadge
// ──────────────────────────────────────────────

export interface PillProps {
  /** Visual tone determines color */
  tone: PillTone;
  /** Label text (mono 10px uppercase) */
  children: string;
}

// ──────────────────────────────────────────────
// SectionLabel
// ──────────────────────────────────────────────

export interface SectionLabelProps {
  /** Label text (mono 10px uppercase) */
  children: string;
}

// ──────────────────────────────────────────────
// TwoCol
// ──────────────────────────────────────────────

export interface TwoColProps {
  /** Left column content */
  left: ReactNode;
  /** Right column content */
  right: ReactNode;
}

// ──────────────────────────────────────────────
// ViewSkeleton
// ──────────────────────────────────────────────

export interface ViewSkeletonProps {
  /** Which view the skeleton represents */
  viewId: string;
}

// ──────────────────────────────────────────────
// ErrorState
// ──────────────────────────────────────────────

export interface ErrorStateProps {
  /** View-level error title */
  title?: string;
  /** Error description */
  message: string;
  /** Retry callback */
  onRetry?: () => void;
}

// ──────────────────────────────────────────────
// ZeroState
// ──────────────────────────────────────────────

export interface ZeroStateProps {
  /** Mono label (e.g. "NO VIOLATIONS") */
  label: string;
  /** Body description explaining what "empty" means */
  description: string;
  /** Optional call-to-action */
  action?: {
    label: string;
    href?: string;
    onClick?: () => void;
  };
}
