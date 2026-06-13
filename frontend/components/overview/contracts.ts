/**
 * Contract Freeze: Overview Component Props
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#components
 * Source: design/components.md
 */
import type { GovernanceHealth } from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// ScoreRing
// ──────────────────────────────────────────────

export interface ScoreRingProps {
  /** Score value (0-100) */
  score: number;
  /** Optional size override (default: 160) */
  size?: number;
}

// ──────────────────────────────────────────────
// DimensionBar
// ──────────────────────────────────────────────

export interface DimensionBarItem {
  /** Label (mono 11px uppercase, 100px width) */
  label: string;
  /** Display value (mono 12px tabular-nums) */
  value: number | string;
  /** Percentage fill (0-100) */
  pct: number;
  /** Color tone for the bar fill */
  tone?: 'fg' | 'accent' | 'success' | 'warn' | 'danger';
}

export interface DimensionBarProps {
  /** Array of dimension bars to render */
  items: DimensionBarItem[];
}

// ──────────────────────────────────────────────
// Overview View
// ──────────────────────────────────────────────

export interface OverviewViewProps {
  /** Fetched governance health data */
  data: GovernanceHealth;
}
