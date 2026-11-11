import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getNotifications, markAllRead, markNotificationRead, type Notification } from '../../api/petlife';
import { Badge, Button, EmptyState, LoadingState } from '../../components/ui';

const ICONS: Record<string, string> = {
  HEALTH_REMINDER: '🩺', ACHIEVEMENT: '🏅', CHALLENGE: '🎯', SOCIAL: '💬',
  PLAYDATE: '🐶', LOST_PET: '⚠️', BOOKING: '📅', SYSTEM: 'ℹ️',
};

function linkFor(n: Notification): string | null {
  const d = (n.data ?? {}) as Record<string, unknown>;
  if (d.petId) return `/app/pets/${d.petId}`;
  if (n.type === 'LOST_PET' || n.type === 'PLAYDATE') return '/app/map';
  if (n.type === 'BOOKING' || n.type === 'ACHIEVEMENT' || n.type === 'CHALLENGE') return '/app/discover';
  return null;
}

export function NotificationsPage() {
  const [items, setItems] = useState<Notification[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getNotifications().then((r) => setItems(r.items ?? [])).catch((e) => setError(e instanceof Error ? e.message : 'Could not load notifications'));
  }, []);

  async function markRead(n: Notification) {
    if (n.read || !n.id) return;
    await markNotificationRead(n.id).catch(() => {});
    setItems((list) => list?.map((i) => (i.id === n.id ? { ...i, read: true } : i)) ?? list);
  }

  if (items === null && !error) return <LoadingState label="Loading notifications…" />;

  return (
    <div className="mx-auto max-w-2xl space-y-4">
      <div className="flex items-center justify-between">
        <h1 className="text-heading">Notifications</h1>
        {(items ?? []).some((n) => !n.read) ? (
          <Button variant="ghost" size="sm" onClick={() => markAllRead().then(() => setItems((l) => l?.map((i) => ({ ...i, read: true })) ?? l)).catch(() => {})}>
            Mark all as read
          </Button>
        ) : null}
      </div>
      {error ? <p role="alert" className="pl-card p-4 font-bold text-danger">{error}</p> : null}
      {(items ?? []).length === 0 && !error ? <EmptyState title="All caught up" description="Reminders, achievements and social updates will show up here." /> : null}
      <div className="space-y-2">
        {(items ?? []).map((n) => {
          const to = linkFor(n);
          const inner = (
            <div className={`pl-card flex items-start gap-3 p-4 ${n.read ? 'opacity-70' : ''}`}>
              <span className="text-2xl" aria-hidden="true">{ICONS[n.type ?? 'SYSTEM']}</span>
              <div className="min-w-0 flex-1">
                <div className="flex items-center gap-2">
                  <p className="font-extrabold text-ink">{n.title}</p>
                  {!n.read ? <Badge tone="accent">New</Badge> : null}
                </div>
                <p className="text-body text-muted">{n.body}</p>
                <p className="mt-1 text-caption text-muted">{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</p>
              </div>
            </div>
          );
          return to
            ? <Link key={n.id} to={to} onClick={() => void markRead(n)}>{inner}</Link>
            : <button key={n.id} type="button" className="block w-full text-left" onClick={() => void markRead(n)}>{inner}</button>;
        })}
      </div>
    </div>
  );
}
