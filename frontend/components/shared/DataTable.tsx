import type { ReactNode } from 'react';
import type { PillTone } from '@/lib/contracts/types';

interface Column {
  key: string;
  label: string;
  mono?: boolean;
  numeric?: boolean;
  width?: string;
}

interface DataTableProps {
  columns: Column[];
  rows: Record<string, ReactNode>[];
  caption?: string;
}

const PILL_COLORS: Record<PillTone, { text: string; border: string; bg: string }> = {
  critical: { text: 'text-danger', border: 'border-danger', bg: 'bg-danger/10' },
  high: { text: 'text-warn', border: 'border-warn', bg: 'bg-warn/10' },
  low: { text: 'text-success', border: 'border-success', bg: 'bg-success/10' },
  info: { text: 'text-muted', border: 'border-border', bg: 'bg-bg' },
  pass: { text: 'text-success', border: 'border-success', bg: 'bg-success/5' },
  fail: { text: 'text-danger', border: 'border-danger', bg: 'bg-danger/5' },
  warn: { text: 'text-warn', border: 'border-warn', bg: 'bg-warn/5' },
};

/**
 * Full-width table with collapsed borders.
 * Header: 2px bottom border, mono 10px uppercase.
 * Row: 1px bottom border. Hover: bg-bg.
 */
export function DataTable({ columns, rows, caption }: DataTableProps) {
  return (
    <div className="w-full overflow-x-auto">
      <table className="w-full border-collapse">
        <thead>
          <tr className="border-b-2 border-fg">
            {columns.map((col) => (
              <th
                key={col.key}
                className={`px-4 pb-2 text-left font-mono text-[10px] font-medium uppercase tracking-[0.08em] text-fg ${
                  col.numeric ? 'tabular-nums' : ''
                }`}
                style={{ width: col.width }}
              >
                {col.label}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.length === 0 ? (
            <tr>
              <td
                colSpan={columns.length}
                className="px-4 py-8 text-center font-mono text-[10px] uppercase tracking-[0.06em] text-muted"
              >
                No data
              </td>
            </tr>
          ) : (
            rows.map((row, i) => (
              <tr key={i} className="border-b border-border hover:bg-bg">
                {columns.map((col) => (
                  <td
                    key={col.key}
                    className={`px-4 py-3 text-body ${
                      col.mono
                        ? 'font-mono text-[12px] tracking-[0.02em]'
                        : ''
                    } ${col.numeric ? 'tabular-nums' : ''}`}
                  >
                    {row[col.key]}
                  </td>
                ))}
              </tr>
            ))
          )}
        </tbody>
      </table>
      {caption && (
        <p className="mt-2 font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
          {caption}
        </p>
      )}
    </div>
  );
}

// ──────────────────────────────────────────────
// Pill / StatusBadge
// ──────────────────────────────────────────────

interface PillProps {
  tone: PillTone;
  children: string;
}

export function Pill({ tone, children }: PillProps) {
  const colors = PILL_COLORS[tone];
  return (
    <span
      className={`inline-flex items-center rounded-none border px-2 py-0.5 font-mono text-[10px] uppercase tracking-[0.06em] ${colors.text} ${colors.border} ${colors.bg}`}
    >
      {children}
    </span>
  );
}
