import type { PolicySummary, Policy } from '@/lib/contracts/types';
import { StatGrid } from '@/components/shared/StatGrid';
import { SectionLabel } from '@/components/shared/Utilities';
import type { StatItem } from '@/components/shared/contracts';

interface RuleCardProps {
  name: string;
  description: string;
  scope: string;
  violationCount: number;
  violatingServices: string[];
  status: 'passing' | 'violated';
}

interface PolicyViewProps {
  data?: PolicySummary;
}

/**
 * RuleCard — Policy rule card with 3px left border.
 * Violated: red border. Passing: green border.
 */
function RuleCard({
  name,
  description,
  scope,
  violationCount,
  violatingServices,
  status,
}: RuleCardProps) {
  const isViolated = status === 'violated';

  return (
    <div
      className={`border-l-3 bg-surface px-5 py-4 ${
        isViolated ? 'border-danger' : 'border-success'
      }`}
    >
      <div className="flex items-start justify-between">
        <div className="flex-1">
          <p className="font-mono text-[12px] font-medium tracking-[0.02em] text-fg">
            {name}
          </p>
          {description && (
            <p className="mt-1 text-body text-muted">{description}</p>
          )}
        </div>
        <span
          className={`inline-flex shrink-0 border px-2 py-0.5 font-mono text-[10px] uppercase tracking-[0.06em] ${
            isViolated
              ? 'border-danger text-danger bg-danger/5'
              : 'border-success text-success bg-success/5'
          }`}
        >
          {isViolated ? `${violationCount} violations` : 'passing'}
        </span>
      </div>

      <div className="mt-3 flex gap-4">
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Scope</p>
          <p className="mt-0.5 font-mono text-[11px] text-fg">{scope}</p>
        </div>
        {isViolated && violatingServices.length > 0 && (
          <div>
            <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Violating</p>
            <div className="mt-0.5 flex flex-wrap gap-1">
              {violatingServices.map((svc, i) => (
                <span key={i} className="font-mono text-[11px] text-fg">
                  {i > 0 && <span className="mx-0.5 text-muted">·</span>}
                  {svc}
                </span>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  );
}

/**
 * Policy Compliance view.
 */
export function PolicyView({ data }: PolicyViewProps) {
  if (!data || data.policies.length === 0) {
    return <p className="text-body text-muted">No policies defined.</p>;
  }

  const stats: StatItem[] = [
    { value: data.activePolicies, label: 'Active Policies' },
    { value: `${data.passRate}%`, label: 'Pass Rate', tone: data.passRate >= 90 ? 'success' : data.passRate >= 70 ? 'warn' : 'danger' },
    { value: data.openViolations, label: 'Open Violations', tone: data.openViolations > 0 ? 'danger' : 'success' },
    { value: data.coveredApis, label: 'APIs Covered' },
  ];

  const violatedPolicies = data.policies.filter((p) => p.status === 'violated');
  const passingPolicies = data.policies.filter((p) => p.status === 'passing');

  return (
    <div className="flex flex-col gap-6">
      <StatGrid stats={stats} />

      {violatedPolicies.length > 0 && (
        <>
          <SectionLabel>Violated Rules</SectionLabel>
          <div className="flex flex-col gap-3">
            {violatedPolicies.map((policy) => (
              <RuleCard key={policy.id} {...policy} />
            ))}
          </div>
        </>
      )}

      {passingPolicies.length > 0 && (
        <>
          <SectionLabel>Passing Rules</SectionLabel>
          <div className="flex flex-col gap-3">
            {passingPolicies.map((policy) => (
              <RuleCard key={policy.id} {...policy} />
            ))}
          </div>
        </>
      )}
    </div>
  );
}
