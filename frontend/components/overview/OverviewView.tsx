'use client';

import type { GovernanceHealth } from '@/lib/contracts/types';
import { ViewShell } from '@/components/shared/ViewShell';
import { StatGrid } from '@/components/shared/StatGrid';
import { TwoCol } from '@/components/shared/Utilities';
import { DataTable } from '@/components/shared/DataTable';
import type { StatItem } from '@/components/shared/contracts';
import type { StatItem } from '@/components/shared/contracts';

interface OverviewViewProps {
  data?: GovernanceHealth;
}

/**
 * ScoreRing — SVG donut chart (160×160, accent arc, score centre).
 */
function ScoreRing({ score, size = 160 }: { score: number; size?: number }) {
  const radius = 68;
  const circumference = 2 * Math.PI * radius; // 427
  const offset = circumference * (1 - Math.min(score, 100) / 100);

  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 160 160"
      className="shrink-0"
      role="img"
      aria-label={`Health score: ${score} out of 100`}
    >
      {/* Background ring */}
      <circle
        cx="80"
        cy="80"
        r={radius}
        fill="none"
        stroke="var(--border)"
        strokeWidth={8}
      />
      {/* Progress arc */}
      <circle
        cx="80"
        cy="80"
        r={radius}
        fill="none"
        stroke="var(--accent)"
        strokeWidth={8}
        strokeLinecap="round"
        strokeDasharray={circumference}
        strokeDashoffset={offset}
        transform="rotate(-90 80 80)"
      />
      {/* Center text */}
      <text
        x="80"
        y="72"
        textAnchor="middle"
        fill="var(--accent)"
        fontFamily="var(--font-display)"
        fontSize="44"
        fontWeight="400"
      >
        {Math.round(score)}
      </text>
      <text
        x="80"
        y="90"
        textAnchor="middle"
        fill="var(--muted)"
        fontFamily="var(--font-mono)"
        fontSize="10"
        letterSpacing="0.08em"
      >
        SCORE
      </text>
    </svg>
  );
}

/**
 * DimensionBar — Horizontal bar with label + fill + value.
 */
function DimensionBar({
  items,
}: {
  items: Array<{
    label: string;
    value: number | string;
    pct: number;
    tone?: 'fg' | 'accent' | 'success' | 'warn' | 'danger';
  }>;
}) {
  const toneColors: Record<string, string> = {
    fg: 'var(--fg)',
    accent: 'var(--accent)',
    success: 'var(--success)',
    warn: 'var(--warn)',
    danger: 'var(--danger)',
  };

  return (
    <div className="flex flex-col gap-4">
      {items.map((item, i) => (
        <div key={i} className="flex items-center gap-4">
          <span className="w-[100px] text-right font-mono text-[11px] uppercase tracking-[0.06em] text-muted">
            {item.label}
          </span>
          <div className="relative h-1.5 flex-1 bg-border">
            <div
              className="absolute inset-y-0 left-0 h-1.5"
              style={{
                width: `${Math.min(item.pct, 100)}%`,
                backgroundColor: item.tone ? toneColors[item.tone] : 'var(--fg)',
              }}
            />
          </div>
          <span className="w-12 text-right font-mono text-[12px] tabular-nums text-fg">
            {item.value}
          </span>
        </div>
      ))}
    </div>
  );
}

/**
 * Overview view — aggregate governance health.
 */
export function OverviewView({ data }: OverviewViewProps) {
  if (!data) {
    return <p className="text-body text-muted">No governance data available.</p>;
  }

  const stats: StatItem[] = [
    { value: data.summary.totalApis, label: 'Total APIs' },
    { value: data.summary.activePolicies, label: 'Active Policies' },
    { value: data.summary.breakingChanges30d, label: 'Breaking Changes (30d)', tone: data.summary.breakingChanges30d > 0 ? 'danger' : 'success' },
    { value: data.summary.servicesAtRisk, label: 'Services at Risk', tone: data.summary.servicesAtRisk > 0 ? 'warn' : undefined },
    { value: data.summary.dependencyEdges, label: 'Dependency Edges' },
  ];

  const dimensions = [
    { label: 'Compliance', value: `${data.dimensions.compliance.pct}%`, pct: data.dimensions.compliance.pct, tone: 'success' as const },
    { label: 'Breaking', value: `${data.dimensions.breaking.pct}%`, pct: data.dimensions.breaking.pct, tone: data.dimensions.breaking.pct > 50 ? 'danger' : 'warn' as const },
    { label: 'Coverage', value: `${data.dimensions.coverage.pct}%`, pct: data.dimensions.coverage.pct, tone: 'accent' as const },
    { label: 'Staleness', value: `${data.dimensions.staleness.pct}%`, pct: data.dimensions.staleness.pct, tone: 'fg' as const },
    { label: 'Impact', value: `${data.dimensions.impact.pct}%`, pct: data.dimensions.impact.pct, tone: 'success' as const },
  ];

  const recentColumns = [
    { key: 'serviceName', label: 'Service', mono: true },
    { key: 'changeType', label: 'Change Type', mono: true },
    { key: 'severity', label: 'Severity', mono: true },
    { key: 'relativeTime', label: 'When', mono: true },
  ];

  const violationColumns = [
    { key: 'serviceName', label: 'Service', mono: true },
    { key: 'policyName', label: 'Policy', mono: true },
    { key: 'violationCount', label: 'Violations', numeric: true },
    { key: 'trend', label: 'Trend', numeric: true },
  ];

  return (
    <div className="flex flex-col gap-8">
      {/* Score ring + Dimension bars */}
      <div className="flex gap-10">
        <ScoreRing score={data.overallScore} />
        <div className="flex-1">
          <DimensionBar items={dimensions} />
        </div>
      </div>

      {/* Stat grid */}
      <StatGrid stats={stats} />

      {/* Recent breakages + Top violations */}
      <TwoCol
        left={
          <div className="flex flex-col gap-3">
            <h2 className="font-mono text-[10px] font-medium uppercase tracking-[0.08em] text-fg">
              Recent Breakages
            </h2>
            <DataTable
              columns={recentColumns}
              rows={data.recentBreakages.map((b) => ({
                ...b,
                severity: b.severity === 'critical'
                  ? <span className="text-danger">{b.severity}</span>
                  : <span className="text-warn">{b.severity}</span>,
                changeType: b.changeType.replace(/-/g, ' '),
              }))}
            />
          </div>
        }
        right={
          <div className="flex flex-col gap-3">
            <h2 className="font-mono text-[10px] font-medium uppercase tracking-[0.08em] text-fg">
              Top Policy Violations
            </h2>
            <DataTable
              columns={violationColumns}
              rows={data.topViolations.map((v) => ({
                ...v,
                trend: v.trend > 0
                  ? <span className="text-danger">+{v.trend}</span>
                  : <span className="text-success">{v.trend}</span>,
              }))}
            />
          </div>
        }
      />
    </div>
  );
}
