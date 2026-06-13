import type { ApiInventoryItem, StaleApiItem } from '@/lib/contracts/types';
import { DataTable } from '@/components/shared/DataTable';
import { SectionLabel } from '@/components/shared/Utilities';
import { Pill } from '@/components/shared/DataTable';

interface InventoryViewProps {
  apis?: ApiInventoryItem[];
  staleApis?: StaleApiItem[];
}

const healthToTone = (health: string) => {
  switch (health) {
    case 'healthy': return 'pass' as const;
    case 'low-risk': return 'info' as const;
    case 'warning': return 'warn' as const;
    case 'at-risk': return 'fail' as const;
    default: return 'info' as const;
  }
};

/**
 * API Inventory view.
 */
export function InventoryView({ apis, staleApis }: InventoryViewProps) {
  const apiColumns = [
    { key: 'serviceName', label: 'Service', mono: true },
    { key: 'version', label: 'Version', mono: true },
    { key: 'specFormat', label: 'Format', mono: true },
    { key: 'health', label: 'Health' },
    { key: 'lastAnalyzed', label: 'Last Analyzed', mono: true },
    { key: 'owner', label: 'Owner', mono: true },
    { key: 'policyPassRate', label: 'Pass Rate', numeric: true },
  ];

  const staleColumns = [
    { key: 'serviceName', label: 'Service', mono: true },
    { key: 'lastIngested', label: 'Last Ingested', mono: true },
    { key: 'daysStale', label: 'Days Stale', numeric: true },
    { key: 'version', label: 'Version', mono: true },
  ];

  const apiRows = (apis ?? []).map((api) => ({
    ...api,
    health: <Pill tone={healthToTone(api.health)}>{api.health}</Pill>,
    policyPassRate: api.policyPassRate !== undefined ? `${api.policyPassRate}%` : '—',
  }));

  const staleRows = (staleApis ?? []).map((api) => ({
    ...api,
    daysStale: <span className={api.daysStale > 30 ? 'text-danger' : 'text-warn'}>{api.daysStale}d</span>,
  }));

  return (
    <div className="flex flex-col gap-8">
      <div className="flex flex-col gap-3">
        <SectionLabel>All Specifications</SectionLabel>
        <DataTable
          columns={apiColumns}
          rows={apiRows}
          caption={apis ? `${apis.length} total APIs` : undefined}
        />
      </div>

      {staleApis && staleApis.length > 0 && (
        <div className="flex flex-col gap-3">
          <SectionLabel>Stale Specifications</SectionLabel>
          <DataTable
            columns={staleColumns}
            rows={staleRows}
            caption="APIs past ingestion threshold"
          />
        </div>
      )}
    </div>
  );
}
