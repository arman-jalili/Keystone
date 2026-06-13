'use client';

import { useState } from 'react';
import type { Notification, NotificationChannel } from '@/lib/contracts/types';
import { StatGrid } from '@/components/shared/StatGrid';
import { SectionLabel, TwoCol } from '@/components/shared/Utilities';
import type { StatItem } from '@/components/shared/contracts';

interface NotificationsViewProps {
  notifications?: Notification[];
  channels?: NotificationChannel[];
}

const severityColors: Record<string, string> = {
  critical: 'before:bg-danger',
  high: 'before:bg-warn',
  warning: 'before:bg-border',
};

/**
 * NotificationFeed — Feed items with read/unread tracking.
 */
function NotificationFeed({
  items,
  onMarkRead,
}: {
  items: Notification[];
  onMarkRead?: (id: string) => void;
}) {
  if (items.length === 0) {
    return <p className="text-body text-muted">No notifications.</p>;
  }

  return (
    <div className="flex flex-col">
      {items.map((item) => {
        const isUnread = !item.read;
        return (
          <div
            key={item.id}
            className={`flex gap-4 border-b border-border py-3.5 ${
              isUnread ? 'cursor-pointer' : ''
            }`}
            onClick={() => isUnread && onMarkRead?.(item.id)}
          >
            {/* Unread dot */}
            <div className="flex shrink-0 items-start pt-1.5">
              <span
                className={`block h-2 w-2 rounded-full ${
                  isUnread ? 'bg-accent' : 'bg-border'
                }`}
              />
            </div>

            {/* Content */}
            <div className="flex-1">
              <div className="flex items-center gap-4">
                <p className={`text-body ${isUnread ? 'font-[510]' : ''}`}>
                  {item.title}
                </p>
                <span
                  className={`inline-flex border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-[0.06em] ${
                    item.severity === 'critical'
                      ? 'border-danger text-danger bg-danger/5'
                      : item.severity === 'high'
                        ? 'border-warn text-warn bg-warn/5'
                        : 'border-border text-muted'
                  }`}
                >
                  {item.severity}
                </span>
              </div>
              <p className="mt-0.5 text-body text-muted">{item.description}</p>
              <div className="mt-1 flex gap-4">
                <span className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">
                  via {item.channel}
                </span>
                <span className="font-mono text-[10px] tracking-[0.06em] text-muted">
                  {item.channelDetail}
                </span>
                <span className="font-mono text-[10px] tracking-[0.06em] text-muted">
                  {item.relativeTime}
                </span>
              </div>
            </div>
          </div>
        );
      })}
    </div>
  );
}

/**
 * ChannelCard — Channel configuration display card.
 */
function ChannelCard({ channel }: { channel: NotificationChannel }) {
  const isActive = channel.status === 'active';
  return (
    <div className="border border-border bg-surface px-6 py-5">
      <div className="flex items-center justify-between">
        <div className="flex items-center gap-3">
          <span className="font-mono text-[12px] font-medium tracking-[0.02em] text-fg">
            {channel.type === 'slack' ? 'Slack' : channel.type === 'email' ? 'Email' : 'Webhook'}
          </span>
          <span
            className={`inline-flex border px-1.5 py-0.5 font-mono text-[10px] uppercase tracking-[0.06em] ${
              isActive
                ? 'border-success text-success'
                : 'border-border text-muted'
            }`}
          >
            {channel.status}
          </span>
        </div>
      </div>

      <div className="mt-3 flex flex-col gap-2">
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Target</p>
          <p className="font-mono text-[11px] text-fg">{channel.config.target}</p>
        </div>
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Rules</p>
          <div className="mt-0.5 flex flex-wrap gap-1">
            {channel.config.rules.map((rule, i) => (
              <span key={i} className="font-mono text-[11px] text-fg">
                {i > 0 && <span className="mx-0.5 text-muted">·</span>}
                {rule.replace(/-/g, ' ')}
              </span>
            ))}
          </div>
        </div>
        <div>
          <p className="font-mono text-[10px] uppercase tracking-[0.06em] text-muted">Last Delivered</p>
          <p className="font-mono text-[11px] text-fg">{channel.config.lastDelivered}</p>
        </div>
      </div>
    </div>
  );
}

/**
 * Notifications view.
 */
export function NotificationsView({ notifications, channels }: NotificationsViewProps) {
  const [localNotifications, setLocalNotifications] = useState(notifications ?? []);

  const unreadCount = localNotifications.filter((n) => !n.read).length;
  const activeChannels = channels?.filter((c) => c.status === 'active').length ?? 0;

  const stats: StatItem[] = [
    { value: localNotifications.length, label: 'Total (7d)' },
    { value: unreadCount, label: 'Unread', tone: unreadCount > 0 ? 'accent' : undefined },
    { value: activeChannels, label: 'Active Channels', tone: 'success' },
    { value: channels?.length ?? 0, label: 'Configured' },
  ];

  const handleMarkRead = (id: string) => {
    setLocalNotifications((prev) =>
      prev.map((n) => (n.id === id ? { ...n, read: true } : n)),
    );
  };

  const unreadItems = localNotifications.filter((n) => !n.read);
  const readItems = localNotifications.filter((n) => n.read);

  return (
    <div className="flex flex-col gap-6">
      <StatGrid stats={stats} />

      {unreadItems.length > 0 && (
        <>
          <SectionLabel>Unread</SectionLabel>
          <NotificationFeed items={unreadItems} onMarkRead={handleMarkRead} />
        </>
      )}

      {readItems.length > 0 && (
        <>
          <SectionLabel>Read</SectionLabel>
          <NotificationFeed items={readItems} />
        </>
      )}

      {channels && channels.length > 0 && (
        <>
          <SectionLabel>Channels</SectionLabel>
          <TwoCol
            left={channels[0] ? <ChannelCard channel={channels[0]} /> : null}
            right={channels[1] ? <ChannelCard channel={channels[1]} /> : null}
          />
        </>
      )}
    </div>
  );
}
