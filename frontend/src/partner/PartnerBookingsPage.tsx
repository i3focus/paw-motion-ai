import { useEffect, useState } from 'react';
import { ApiError } from '../api/client';
import { completeBooking, confirmBooking, declinePartnerBooking, listPartnerBookings, type Booking } from '../api/partner';
import { Badge, Button, Card, EmptyState, ErrorMessage, LoadingState, SectionHeader } from '../components/ui';

const STATUS_TONE: Record<string, 'neutral' | 'accent' | 'success' | 'warning' | 'danger'> = {
  REQUESTED: 'warning',
  CONFIRMED: 'success',
  CANCELLED: 'neutral',
  COMPLETED: 'accent',
};

export function PartnerBookingsPage() {
  const [bookings, setBookings] = useState<Booking[] | null>(null);
  const [hasNext, setHasNext] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [loadingMore, setLoadingMore] = useState(false);

  const reload = () => listPartnerBookings()
    .then((r) => { setBookings(r.items ?? []); setHasNext(Boolean(r.hasNext)); })
    .catch((e) => { setError(e instanceof ApiError ? e.message : 'Unable to load bookings'); setBookings([]); });
  useEffect(() => { reload(); }, []);

  async function loadMore() {
    const page = Math.ceil((bookings?.length ?? 0) / 30);
    setLoadingMore(true);
    try {
      const r = await listPartnerBookings(page);
      setBookings((b) => [...(b ?? []), ...(r.items ?? [])]);
      setHasNext(Boolean(r.hasNext));
    } catch (e) {
      setError(e instanceof ApiError ? e.message : 'Unable to load more bookings');
    } finally {
      setLoadingMore(false);
    }
  }

  async function act(b: Booking, action: 'confirm' | 'complete' | 'decline') {
    if (!b.id) return;
    setBusy(b.id);
    setError(null);
    try {
      await (action === 'confirm' ? confirmBooking(b.id) : action === 'complete' ? completeBooking(b.id) : declinePartnerBooking(b.id));
      await reload();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to update booking');
    } finally {
      setBusy(null);
    }
  }

  const upcoming = (bookings ?? []).filter((b) => b.status === 'REQUESTED' || b.status === 'CONFIRMED');
  const past = (bookings ?? []).filter((b) => b.status === 'CANCELLED' || b.status === 'COMPLETED');

  const row = (b: Booking) => (
    <div key={b.id} className="flex flex-wrap items-center gap-3 rounded-2xl border border-line bg-cream p-4">
      <div className="min-w-0 flex-1">
        <div className="flex flex-wrap items-center gap-2">
          <h3 className="font-bold text-ink">{b.serviceName}</h3>
          <Badge tone={STATUS_TONE[b.status ?? ''] ?? 'neutral'}>{b.status?.toLowerCase()}</Badge>
          {b.source === 'AI' ? <Badge tone="accent">via PetLife AI</Badge> : b.source === 'AD' ? <Badge tone="accent">via ad</Badge> : null}
        </div>
        <p className="text-label text-muted">
          {b.petName} · {b.ownerName} · {new Date(b.startsAt ?? '').toLocaleString()} · {((b.priceCents ?? 0) / 100).toFixed(2)} {b.currency}
        </p>
        {b.notes ? <p className="mt-1 text-body text-muted">“{b.notes}”</p> : null}
      </div>
      {b.status === 'REQUESTED' ? <Button onClick={() => act(b, 'confirm')} disabled={busy === b.id}>Confirm</Button> : null}
      {b.status === 'REQUESTED' || b.status === 'CONFIRMED' ? (
        <Button variant="ghost" onClick={() => { if (confirm(`Decline the booking for ${b.petName}?`)) act(b, 'decline'); }} disabled={busy === b.id}>
          {b.status === 'REQUESTED' ? 'Decline' : 'Cancel'}
        </Button>
      ) : null}
      {b.status === 'CONFIRMED' ? <Button variant="secondary" onClick={() => act(b, 'complete')} disabled={busy === b.id}>Mark completed</Button> : null}
    </div>
  );

  return (
    <div className="space-y-6">
      <SectionHeader title="Bookings" subtitle="Reservations made by owners or scheduled by PetLife AI — confirm them to lock the slot." />
      <ErrorMessage message={error} />
      {bookings === null ? <LoadingState /> : bookings.length === 0 ? (
        <Card><EmptyState title="No bookings yet" description="When owners book your services — or PetLife AI schedules one — they appear here." /></Card>
      ) : (
        <div className="space-y-6">
          {upcoming.length > 0 ? <Card title="Upcoming"><div className="space-y-3">{upcoming.map(row)}</div></Card> : null}
          {past.length > 0 ? <Card title="History"><div className="space-y-3">{past.map(row)}</div></Card> : null}
          {hasNext ? <Button variant="secondary" onClick={loadMore} disabled={loadingMore} className="w-full">{loadingMore ? 'Loading…' : 'Load more'}</Button> : null}
        </div>
      )}
    </div>
  );
}
