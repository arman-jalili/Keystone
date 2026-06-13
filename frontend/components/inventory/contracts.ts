/**
 * Contract Freeze: API Inventory Component Props
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#components
 * Source: design/components.md
 */
import type { ApiInventoryItem, StaleApiItem } from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// ApiTable
// ──────────────────────────────────────────────

export interface ApiTableProps {
  /** Full list of API inventory items */
  items: ApiInventoryItem[];
}

// ──────────────────────────────────────────────
// StaleApiTable
// ──────────────────────────────────────────────

export interface StaleApiTableProps {
  /** List of stale API items */
  items: StaleApiItem[];
}
