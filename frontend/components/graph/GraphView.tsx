'use client';

import { useState } from 'react';
import type { DependencyGraphData, ImpactCascade } from '@/lib/contracts/types';
import { SectionLabel } from '@/components/shared/Utilities';

interface GraphViewProps {
  graphData?: DependencyGraphData;
  cascades?: ImpactCascade[];
}

/**
 * DependencyGraph — SVG interactive graph with hover states.
 * Renders rect nodes + line edges. Impacted nodes get danger stroke/tint.
 */
function DependencyGraph({
  graphData,
}: {
  graphData?: DependencyGraphData;
}) {
  const [hoveredNode, setHoveredNode] = useState<string | null>(null);

  if (!graphData || graphData.nodes.length === 0) {
    return <p className="text-body text-muted">No graph data available.</p>;
  }

  const padding = 40;
  const width = 900;
  const height = 420;

  return (
    <div className="border border-border bg-surface">
      <svg
        viewBox={`0 0 ${width} ${height}`}
        className="w-full"
        style={{ height: `${height}px` }}
      >
        {/* Edges */}
        {graphData.edges.map((edge, i) => {
          const from = graphData.nodes.find((n) => n.id === edge.from);
          const to = graphData.nodes.find((n) => n.id === edge.to);
          if (!from || !to) return null;

          return (
            <line
              key={`edge-${i}`}
              x1={from.x}
              y1={from.y}
              x2={to.x}
              y2={to.y}
              stroke={edge.impacted ? 'var(--danger)' : 'var(--border)'}
              strokeWidth={edge.impacted ? 2 : 1.5}
            />
          );
        })}

        {/* Nodes */}
        {graphData.nodes.map((node) => {
          const isHovered = hoveredNode === node.id;
          const strokeColor = node.impacted
            ? 'var(--danger)'
            : isHovered
              ? 'var(--accent)'
              : 'var(--border)';

          return (
            <g
              key={node.id}
              onMouseEnter={() => setHoveredNode(node.id)}
              onMouseLeave={() => setHoveredNode(null)}
              style={{ cursor: 'pointer' }}
            >
              <rect
                x={node.x - 60}
                y={node.y - 20}
                width={120}
                height={40}
                rx={2}
                fill={node.impacted ? 'color-mix(in srgb, var(--danger) 10%, var(--surface))' : 'var(--surface)'}
                stroke={strokeColor}
                strokeWidth={isHovered || node.impacted ? 2 : 1}
              />
              <text
                x={node.x}
                y={node.y - 2}
                textAnchor="middle"
                fill="var(--fg)"
                fontFamily="var(--font-display)"
                fontSize="13"
              >
                {node.label}
              </text>
              <text
                x={node.x}
                y={node.y + 12}
                textAnchor="middle"
                fill="var(--muted)"
                fontFamily="var(--font-mono)"
                fontSize="10"
              >
                {node.subtitle}
              </text>
            </g>
          );
        })}
      </svg>

      {/* Legend */}
      <div className="flex gap-6 border-t border-border px-6 py-3">
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 border border-border bg-surface" />
          <span className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Healthy</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="h-3 w-3 border border-danger" style={{ backgroundColor: 'color-mix(in srgb, var(--danger) 10%, var(--surface))' }} />
          <span className="font-mono text-[10px] uppercase tracking-[0.06em] text-danger">Impacted</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="h-0.5 w-6 bg-border" />
          <span className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Dependency</span>
        </div>
        <div className="flex items-center gap-2">
          <span className="h-0.5 w-6 bg-danger" />
          <span className="font-mono text-[10px] uppercase tracking-[0.06em] text-danger">Impact Path</span>
        </div>
      </div>
    </div>
  );
}

/**
 * ImpactCascade — Blast radius summary card.
 */
function ImpactCascadeCard({
  cascade,
}: {
  cascade: ImpactCascade;
}) {
  return (
    <div className="border border-border bg-surface px-6 py-5">
      <div className="flex items-center justify-between">
        <div>
          <p className="font-mono text-[12px] font-medium tracking-[0.02em] text-fg">
            {cascade.sourceService}
          </p>
          <p className="mt-0.5 text-body text-muted">{cascade.changeDescription}</p>
        </div>
        <span
          className={`inline-flex border px-2 py-0.5 font-mono text-[10px] uppercase tracking-[0.06em] ${
            cascade.severity === 'critical'
              ? 'border-danger text-danger bg-danger/5'
              : 'border-warn text-warn bg-warn/5'
          }`}
        >
          {cascade.severity}
        </span>
      </div>

      <div className="mt-3 flex gap-4">
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Version</p>
          <p className="mt-0.5 font-mono text-[11px] text-fg">{cascade.sourceVersion}</p>
        </div>
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Total Consumers</p>
          <p className="mt-0.5 font-mono text-[11px] tabular-nums text-fg">{cascade.totalConsumers}</p>
        </div>
      </div>

      {cascade.downstreamServices.length > 0 && (
        <div className="mt-3">
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Downstream Services</p>
          <div className="mt-1 flex flex-wrap gap-1">
            {cascade.downstreamServices.map((svc, i) => (
              <span key={i} className="font-mono text-[11px] text-danger">
                {i > 0 && <span className="mx-0.5 text-muted">→</span>}
                {svc}
              </span>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

/**
 * Dependency Graph view.
 */
export function GraphView({ graphData, cascades }: GraphViewProps) {
  return (
    <div className="flex flex-col gap-6">
      <DependencyGraph graphData={graphData} />

      {cascades && cascades.length > 0 && (
        <>
          <SectionLabel>Impact Cascade</SectionLabel>
          <div className="flex flex-col gap-3">
            {cascades.map((cascade) => (
              <ImpactCascadeCard key={cascade.id} cascade={cascade} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
