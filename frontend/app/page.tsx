'use client';

import { useState, useCallback, useEffect } from 'react';
import { AppLayout } from '@/components/layout/AppLayout';
import { NavRail } from '@/components/layout/NavRail';
import { isValidViewId, DEFAULT_VIEW, VIEW_REGISTRY } from '@/lib/contracts/view-config';
import type { ViewId } from '@/lib/contracts/types';
import type { GovernanceHealth, BreakingChangeSummary, PolicySummary, ApiInventoryItem, StaleApiItem, DependencyGraphData, ImpactCascade, Notification, NotificationChannel } from '@/lib/contracts/types';
import { OverviewView } from '@/components/overview/OverviewView';
import { InventoryView } from '@/components/inventory/InventoryView';
import { BreakingView } from '@/components/breaking/BreakingView';
import { PolicyView } from '@/components/policy/PolicyView';
import { GraphView } from '@/components/graph/GraphView';
import { NotificationsView } from '@/components/notifications/NotificationsView';
import { ViewSkeleton, ErrorState, ZeroState } from '@/components/shared/Utilities';

// ──────────────────────────────────────────────
// Data cache — one flight per view switch
// ──────────────────────────────────────────────

interface DataCache {
  governanceHealth?: GovernanceHealth;
  apiInventory?: ApiInventoryItem[];
  staleApis?: StaleApiItem[];
  breakingSummary?: BreakingChangeSummary;
  policySummary?: PolicySummary;
  graphData?: DependencyGraphData;
  cascades?: ImpactCascade[];
  notifications?: Notification[];
  channels?: NotificationChannel[];
}

type AsyncState<T> =
  | { status: 'loading' }
  | { status: 'success'; data: T }
  | { status: 'error'; message: string };

async function fetchForView(view: ViewId): Promise<DataCache> {
  const { fetchGovernanceHealth, fetchBreakingChanges, fetchPolicies, fetchApiInventory, fetchStaleApis, fetchDependencyGraph, fetchImpactCascades, fetchNotifications, fetchNotificationChannels } = await import('@/lib/api/data-service');

  switch (view) {
    case 'overview': {
      const governanceHealth = await fetchGovernanceHealth();
      return { governanceHealth };
    }
    case 'inventory': {
      const [apiInventory, staleApis] = await Promise.all([
        fetchApiInventory(),
        fetchStaleApis(),
      ]);
      return { apiInventory, staleApis };
    }
    case 'breaking': {
      const breakingSummary = await fetchBreakingChanges();
      return { breakingSummary };
    }
    case 'policy': {
      const policySummary = await fetchPolicies();
      return { policySummary };
    }
    case 'graph': {
      const [graphData, cascades] = await Promise.all([
        fetchDependencyGraph(),
        fetchImpactCascades(),
      ]);
      return { graphData, cascades };
    }
    case 'notifications': {
      const [notifications, channels] = await Promise.all([
        fetchNotifications(),
        fetchNotificationChannels(),
      ]);
      return { notifications, channels };
    }
  }
}

/**
 * Root page. Reads ?view= search param and renders the active view.
 * Fetches data from the Keystone backend API on view switch.
 */
export default function HomePage() {
  const [activeView, setActiveView] = useState<ViewId>(() => {
    if (typeof window === 'undefined') return DEFAULT_VIEW;
    const params = new URLSearchParams(window.location.search);
    const v = params.get('view');
    return isValidViewId(v) ? v : DEFAULT_VIEW;
  });

  const [data, setData] = useState<DataCache>({});
  const [asyncState, setAsyncState] = useState<AsyncState<DataCache>>({ status: 'loading' });

  // Fetch data when view changes
  useEffect(() => {
    let cancelled = false;
    setAsyncState({ status: 'loading' });

    fetchForView(activeView).then((result) => {
      if (!cancelled) {
        setData((prev) => ({ ...prev, ...result }));
        setAsyncState({ status: 'success', data: result });
      }
    }).catch((err) => {
      if (!cancelled) {
        setAsyncState({ status: 'error', message: (err as Error).message });
      }
    });

    return () => { cancelled = true; };
  }, [activeView]);

  const handleViewChange = useCallback((viewId: ViewId) => {
    setActiveView(viewId);
    const url = new URL(window.location.href);
    url.searchParams.set('view', viewId);
    window.history.pushState({}, '', url.toString());
  }, []);

  const handleRetry = useCallback(() => {
    setAsyncState({ status: 'loading' });
    fetchForView(activeView).then((result) => {
      setData((prev) => ({ ...prev, ...result }));
      setAsyncState({ status: 'success', data: result });
    }).catch((err) => {
      setAsyncState({ status: 'error', message: (err as Error).message });
    });
  }, [activeView]);

  const viewDef = VIEW_REGISTRY[activeView];

  function renderView() {
    if (asyncState.status === 'loading') {
      return <ViewSkeleton viewId={activeView} />;
    }

    if (asyncState.status === 'error') {
      return (
        <ErrorState
          message={asyncState.message}
          onRetry={handleRetry}
        />
      );
    }

    switch (activeView) {
      case 'overview':
        return <OverviewView data={data.governanceHealth} />;
      case 'inventory':
        return <InventoryView apis={data.apiInventory} staleApis={data.staleApis} />;
      case 'breaking':
        return <BreakingView data={data.breakingSummary} />;
      case 'policy':
        return <PolicyView data={data.policySummary} />;
      case 'graph':
        return <GraphView graphData={data.graphData} cascades={data.cascades} />;
      case 'notifications':
        return <NotificationsView notifications={data.notifications} channels={data.channels} />;
      default:
        return <ErrorState message={`Unknown view: ${activeView}`} onRetry={handleRetry} />;
    }
  }

  return (
    <AppLayout
      nav={
        <NavRail
          activeView={activeView}
          onViewChange={handleViewChange}
        />
      }
      breadcrumb={viewDef.title}
    >
      {renderView()}
    </AppLayout>
  );
}
