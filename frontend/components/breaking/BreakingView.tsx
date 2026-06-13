import type { BreakingChangeSummary } from '@/lib/contracts/types';
import { StatGrid } from '@/components/shared/StatGrid';
import { SectionLabel } from '@/components/shared/Utilities';
import type { StatItem } from '@/components/shared/contracts';

interface BreakageCardProps {
  serviceName: string;
  changeType: 'field-removal' | 'type-change' | 'path-removal' | 'enum-change';
  severity: 'critical' | 'high';
  detectedAt: string;
  versionFrom: string;
  versionTo: string;
  impactedConsumers: string[];
  diffText: string;
}

interface BreakingViewProps {
  data?: BreakingChangeSummary;
}

/**
 * DiffBlock — Raw diff text with -/+ line markers.
 * Removed lines: red + line-through. Added lines: green.
 */
function DiffBlock({ diffText }: { diffText: string }) {
  return (
    <pre className="overflow-x-auto border border-border bg-bg px-5 py-4 font-mono text-[12px] leading-[1.7] tracking-[0.02em] whitespace-pre-wrap">
      {diffText.split('\n').map((line, i) => {
        const isRemoved = line.startsWith('-');
        const isAdded = line.startsWith('+');
        return (
          <span
            key={i}
            className={`block ${
              isRemoved
                ? 'text-danger line-through'
                : isAdded
                  ? 'text-success'
                  : 'text-fg'
            }`}
          >
            {line}
          </span>
        );
      })}
    </pre>
  );
}

/**
 * BreakageCard — Breaking change detail card with meta, diff, consumers.
 */
function BreakageCard({
  serviceName,
  changeType,
  severity,
  detectedAt,
  versionFrom,
  versionTo,
  impactedConsumers,
  diffText,
}: BreakageCardProps) {
  return (
    <div className="border border-border bg-surface">
      {/* Header */}
      <div className="flex items-center justify-between border-b border-border px-6 py-4">
        <div>
          <p className="font-mono text-[12px] font-medium tracking-[0.02em] text-fg">
            {serviceName}
          </p>
          <p className="mt-0.5 font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
            {changeType.replace(/-/g, ' ')}
          </p>
        </div>
        <span
          className={`inline-flex border px-2 py-0.5 font-mono text-[10px] uppercase tracking-[0.06em] ${
            severity === 'critical'
              ? 'border-danger text-danger bg-danger/5'
              : 'border-warn text-warn bg-warn/5'
          }`}
        >
          {severity}
        </span>
      </div>

      {/* Meta row */}
      <div className="flex gap-6 border-b border-border px-6 py-3">
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Detected</p>
          <p className="font-mono text-[11px] text-fg">{detectedAt}</p>
        </div>
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">From</p>
          <p className="font-mono text-[11px] text-fg">{versionFrom}</p>
        </div>
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">To</p>
          <p className="font-mono text-[11px] text-fg">{versionTo}</p>
        </div>
      </div>

      {/* Diff block */}
      <div className="px-6 py-4">
        <DiffBlock diffText={diffText} />
      </div>

      {/* Impacted consumers */}
      {impactedConsumers.length > 0 && (
        <div className="border-t border-border px-6 py-3">
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
            Impacted Consumers ({impactedConsumers.length})
          </p>
          <div className="mt-1 flex flex-wrap gap-2">
            {impactedConsumers.map((svc, i) => (
              <span
                key={i}
                className="font-mono text-[11px] text-fg"
              >
                {i > 0 && <span className="mx-1 text-muted">·</span>}
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
 * Breaking Changes view.
 */
export function BreakingView({ data }: BreakingViewProps) {
  if (!data || data.items.length === 0) {
    return <p className="text-body text-muted">No breaking changes detected.</p>;
  }

  const stats: StatItem[] = [
    { value: data.total30d, label: 'Total (30d)' },
    { value: data.critical, label: 'Critical', tone: data.critical > 0 ? 'danger' : undefined },
    { value: data.high, label: 'High', tone: data.high > 0 ? 'warn' : undefined },
    { value: data.nonBreaking, label: 'Non-Breaking', tone: 'success' },
  ];

  return (
    <div className="flex flex-col gap-6">
      <StatGrid stats={stats} />

      {data.critical > 0 && (
        <>
          <SectionLabel>Critical Breakages</SectionLabel>
          <div className="flex flex-col gap-4">
            {data.items
              .filter((item) => item.severity === 'critical')
              .slice(0, 3)
              .map((item) => (
                <BreakageCard key={item.id} {...item} />
              ))}
          </div>
        </>
      )}

      {data.high > 0 && (
        <>
          <SectionLabel>High Severity</SectionLabel>
          <div className="flex flex-col gap-4">
            {data.items
              .filter((item) => item.severity === 'high')
              .slice(0, 3)
              .map((item) => (
                <BreakageCard key={item.id} {...item} />
              ))}
          </div>
        </>
      )}
    </div>
  );
}
