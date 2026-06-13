/**
 * Contract Freeze: Dependency Graph Component Props
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#components
 * Source: design/components.md, design/data-schema.md
 */
import type { DependencyGraphData, ImpactCascade } from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// DependencyGraph (SVG)
// ──────────────────────────────────────────────

export interface DependencyGraphNode {
  id: string;
  label: string;
  subtitle: string;
  x: number;
  y: number;
  impacted?: boolean;
}

export interface DependencyGraphEdge {
  from: string;
  to: string;
  impacted?: boolean;
}

export interface DependencyGraphProps {
  /** Graph data with positioned nodes and edges */
  data: DependencyGraphData;
  /** SVG viewBox width (default: 900) */
  width?: number;
  /** SVG viewBox height (default: 420) */
  height?: number;
}

// ──────────────────────────────────────────────
// Graph Legend
// ──────────────────────────────────────────────

export interface GraphLegendProps {
  /** Whether any nodes are in impacted state */
  hasImpactedNodes: boolean;
}

// ──────────────────────────────────────────────
// ImpactCascade
// ──────────────────────────────────────────────

export interface ImpactCascadeCardProps {
  /** The impact cascade data */
  cascade: ImpactCascade;
}

// ──────────────────────────────────────────────
// Graph View
// ──────────────────────────────────────────────

export interface GraphViewProps {
  /** Fetched dependency graph data */
  graphData: DependencyGraphData;
  /** Fetched impact cascades */
  cascades: ImpactCascade[];
}
