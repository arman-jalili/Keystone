/**
 * Contract Freeze: Notifications Component Props
 *
 * Canonical Reference: .pi/architecture/modules/frontend-app.md#components
 * Source: design/components.md, design/data-schema.md
 */
import type { Notification, NotificationChannel } from '@/lib/contracts/types';

// ──────────────────────────────────────────────
// NotificationFeed
// ──────────────────────────────────────────────

export interface NotificationItem {
  id: string;
  title: string;
  description: string;
  severity: 'critical' | 'high' | 'warning';
  channel: 'slack' | 'email' | 'webhook';
  channelDetail: string;
  read: boolean;
  timestamp: string;
  relativeTime: string;
}

export interface NotificationFeedProps {
  /** Array of notification items to display */
  items: NotificationItem[];
  /** Callback when a notification is marked as read */
  onMarkRead?: (id: string) => void;
}

// ──────────────────────────────────────────────
// ChannelCard
// ──────────────────────────────────────────────

export interface ChannelCardProps {
  /** Channel type */
  type: 'slack' | 'email' | 'webhook';
  /** Channel status */
  status: 'active' | 'inactive';
  /** Channel configuration */
  config: {
    target: string;
    rules: string[];
    lastDelivered: string;
  };
}

// ──────────────────────────────────────────────
// Notifications View
// ──────────────────────────────────────────────

export interface NotificationsViewProps {
  /** Fetched notifications */
  notifications: Notification[];
  /** Fetched notification channels */
  channels: NotificationChannel[];
}
