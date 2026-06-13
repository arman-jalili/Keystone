import type { StatTone } from '@/lib/contracts/types';

interface StatItem {
  value: string | number;
  label: string;
  tone?: StatTone;
}

interface StatGridProps {
  stats: StatItem[];
}

const TONE_COLORS: Record<StatTone, string> = {
  accent: 'text-accent',
  success: 'text-success',
  danger: 'text-danger',
  warn: 'text-warn',
};

/**
 * CSS grid of stat cells. Auto-fits columns with min 180px width.
 * 1px gap shows border color between cells. No cell background.
 */
export function StatGrid({ stats }: StatGridProps) {
  return (
    <div
      className="grid gap-px"
      style={{
        gridTemplateColumns: 'repeat(auto-fit, minmax(180px, 1fr))',
        background: 'var(--border)',
        border: '1px solid var(--border)',
      }}
    >
      {stats.map((stat, i) => (
        <div key={i} className="bg-surface px-6 py-5">
          <p
            className={`font-display text-stat-value tabular-nums ${
              stat.tone ? TONE_COLORS[stat.tone] : 'text-fg'
            }`}
          >
            {stat.value}
          </p>
          <p className="mt-0.5 font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
            {stat.label}
          </p>
        </div>
      ))}
    </div>
  );
}
