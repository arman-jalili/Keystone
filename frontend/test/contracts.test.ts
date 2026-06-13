import { describe, it, expect } from 'vitest';
import { NAV_ITEMS, VIEW_REGISTRY, isValidViewId, DEFAULT_VIEW } from '@/lib/contracts/view-config';

describe('View Config (contract)', () => {
  it('has exactly 6 views', () => {
    const viewIds = Object.keys(VIEW_REGISTRY);
    expect(viewIds).toHaveLength(6);
    expect(viewIds).toEqual([
      'overview',
      'inventory',
      'breaking',
      'policy',
      'graph',
      'notifications',
    ]);
  });

  it('has matching NAV_ITEMS for all views', () => {
    const navViewIds = NAV_ITEMS.map((item) => item.viewId);
    const registryViewIds = Object.keys(VIEW_REGISTRY);
    expect(navViewIds.sort()).toEqual(registryViewIds.sort());
  });

  it('each nav item has a 2-letter icon', () => {
    for (const item of NAV_ITEMS) {
      expect(item.icon).toHaveLength(2);
    }
  });

  it('isValidViewId validates correctly', () => {
    expect(isValidViewId('overview')).toBe(true);
    expect(isValidViewId('notifications')).toBe(true);
    expect(isValidViewId('unknown')).toBe(false);
    expect(isValidViewId(null)).toBe(false);
  });

  it('default view is overview', () => {
    expect(DEFAULT_VIEW).toBe('overview');
  });
});

describe('Endpoints (contract)', () => {
  it('endpoints file can be imported without errors', async () => {
    const { ENDPOINTS, getEndpointPath } = await import('@/lib/contracts/endpoints');
    expect(Object.keys(ENDPOINTS)).toHaveLength(11);
    expect(getEndpointPath('DASHBOARD_HEALTH')).toBe('/dashboard/health');
  });
});

describe('Types (contract)', () => {
  it('TypeScript types compile without errors', async () => {
    // This test verifies the module can be imported
    const types = await import('@/lib/contracts/types');
    expect(types.ViewId).toBeDefined();
  });
});
