/**
 * Contract Freeze: Policy Compliance Component Props
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#components
 * Source: design/components.md
 */
import type { PolicySummary, Policy } from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// RuleCard
// ──────────────────────────────────────────────

export interface RuleCardProps {
  /** Policy name (kebab-case identifier) */
  name: string;
  /** Policy description */
  description: string;
  /** Target scope (e.g. "all APIs", "user-facing APIs") */
  scope: string;
  /** Current violation count */
  violationCount: number;
  /** Names of services currently violating this policy */
  violatingServices: string[];
  /** Whether the policy is currently passing or violated */
  status: 'passing' | 'violated';
}

// ──────────────────────────────────────────────
// Policy View
// ──────────────────────────────────────────────

export interface PolicyViewProps {
  /** Fetched policy summary data */
  data: PolicySummary;
}

/**
 * Filter options for the policy list.
 */
export interface PolicyFilter {
  /** Filter by policy status */
  status?: 'passing' | 'violated';
  /** Search by name or description */
  search?: string;
  /** Sort field */
  sortBy?: 'name' | 'violationCount';
  /** Sort direction */
  sortOrder?: 'asc' | 'desc';
}
