'use client';

import { useState, useCallback } from 'react';
import { AppLayout } from '@/components/layout/AppLayout';
import { NavRail } from '@/components/layout/NavRail';
import { isValidViewId, DEFAULT_VIEW, VIEW_REGISTRY } from '@/lib/contracts/view-config';
import type { ViewId } from '@/lib/contracts/types';

interface HomePageProps {
  searchParams?: Promise<{ view?: string }>;
}

/**
 * Root page. Reads ?view= search param to determine active view.
 * Falls back to "overview" if invalid or missing.
 */
export default function HomePage({ searchParams }: HomePageProps) {
  // Use a client-side state for view switching
  // Initial value derived from searchParams (handled in useEffect for async)
  const [activeView, setActiveView] = useState<ViewId>(DEFAULT_VIEW);

  const handleViewChange = useCallback((viewId: ViewId) => {
    setActiveView(viewId);
    // Update URL without full navigation
    const url = new URL(window.location.href);
    url.searchParams.set('view', viewId);
    window.history.pushState({}, '', url.toString());
  }, []);

  const viewDef = VIEW_REGISTRY[activeView];

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
      <div className="flex flex-col gap-8">
        <div>
          <h1 className="font-display text-view-title text-fg">
            {viewDef.title}
          </h1>
          <p className="mt-1 text-body text-muted">
            {viewDef.subtitle}
          </p>
        </div>

        {/* View content — to be populated by view-specific components */}
        <div className="text-muted font-mono text-[10px] uppercase tracking-[0.06em]">
          {activeView === 'overview' && 'Overview view — ScoreRing, StatGrid, DimensionBar, tables'}
          {activeView === 'inventory' && 'API Inventory view — DataTable with API listing, Stale APIs'}
          {activeView === 'breaking' && 'Breaking Changes view — StatGrid, BreakageCards, DiffBlock'}
          {activeView === 'policy' && 'Policy Compliance view — StatGrid, RuleCards'}
          {activeView === 'graph' && 'Dependency Graph view — SVG graph, ImpactCascade'}
          {activeView === 'notifications' && 'Notifications view — StatGrid, NotificationFeed, ChannelCards'}
        </div>
      </div>
    </AppLayout>
  );
}
