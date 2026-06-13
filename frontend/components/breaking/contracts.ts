/**
 * Contract Freeze: Breaking Changes Component Props
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#components
 * Source: design/components.md
 */
import type { BreakingChangeSummary } from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// DiffBlock
// ──────────────────────────────────────────────

export interface DiffBlockProps {
  /** Raw diff text with - and + line markers */
  diffText: string;
}

// ──────────────────────────────────────────────
// BreakageCard
// ──────────────────────────────────────────────

export interface BreakageCardProps {
  /** Name of the service with the breaking change */
  serviceName: string;
  /** Type of change detected */
  changeType: 'field-removal' | 'type-change' | 'path-removal' | 'enum-change';
  /** Severity level */
  severity: 'critical' | 'high';
  /** ISO 8601 detection timestamp */
  detectedAt: string;
  /** Previous spec version */
  versionFrom: string;
  /** New spec version */
  versionTo: string;
  /** Names of impacted downstream services */
  impactedConsumers: string[];
  /** Raw diff text */
  diffText: string;
}

// ──────────────────────────────────────────────
// Breaking Changes View
// ──────────────────────────────────────────────

export interface BreakingViewProps {
  /** Fetched breaking change summary */
  data: BreakingChangeSummary;
}
