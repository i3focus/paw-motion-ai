import { useEffect, useState } from 'react';
import { Navigate } from 'react-router-dom';
import { ApiError } from '../../api/client';
import { approveCampaign, getAdminCampaignQueue, rejectCampaign, verifyPartner, type AdminCampaign } from '../../api/petlife';
import { useAuth } from '../../auth';
import { safeHref } from '../../lib/validation';
import { Badge, Button, Card, EmptyState, ErrorMessage, LoadingState, SectionHeader, TextArea } from '../../components/ui';
import { FORMAT_LABEL } from '../../partner/PartnerCampaignsPage';

export function AdminAdsPage() {
  const { owner, loading } = useAuth();
  const [queue, setQueue] = useState<AdminCampaign[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<string | null>(null);
  const [reason, setReason] = useState('');
  const [busy, setBusy] = useState<string | null>(null);

  const reload = () => getAdminCampaignQueue()
    .then((r) => setQueue(r.items ?? []))
    .catch((e) => { setError(e instanceof ApiError ? e.message : 'Unable to load the review queue'); setQueue([]); });

  useEffect(() => {
    if (owner?.role === 'ADMIN') reload();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [owner?.role]);

  if (!loading && owner?.role !== 'ADMIN') return <Navigate to="/app/feed" replace />;

  async function act(c: AdminCampaign, action: 'approve' | 'reject' | 'verify' | 'unverify') {
    const id = c.id ?? '';
    setBusy(id + action);
    setError(null);
    try {
      if (action === 'approve') await approveCampaign(id);
      else if (action === 'reject') await rejectCampaign(id, reason.trim() || undefined);
      else await verifyPartner(c.partnerId ?? '', action === 'verify');
      setRejecting(null);
      setReason('');
      await reload();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Action failed');
    } finally {
      setBusy(null);
    }
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="Ad review" subtitle="Approve sponsored content before it reaches the feed, map or Discover." />
      <ErrorMessage message={error} />
      {queue === null ? <LoadingState /> : queue.length === 0 ? (
        <Card><EmptyState title="Queue is clear" description="No campaigns are waiting for review right now." /></Card>
      ) : (
        <div className="space-y-4">
          {queue.map((c) => (
            <Card key={c.id} className="space-y-3">
              <div className="flex flex-wrap items-start justify-between gap-3">
                <div>
                  <h3 className="text-lg font-bold text-ink">{c.title}</h3>
                  <p className="text-label text-muted">
                    {c.partnerName} {c.partnerVerified ? '· ✓ verified' : '· unverified'} · submitted {c.submittedAt ? new Date(c.submittedAt).toLocaleDateString() : ''} · runs {new Date(c.startsAt ?? '').toLocaleDateString()} → {new Date(c.endsAt ?? '').toLocaleDateString()}
                  </p>
                </div>
                <Badge tone="warning">In review</Badge>
              </div>
              {c.mediaUrl ? <img src={c.mediaUrl} alt="Ad creative" className="h-40 w-full rounded-2xl object-cover" loading="lazy" /> : null}
              <p className="text-body text-ink">{c.description}</p>
              <div className="flex flex-wrap gap-1.5">
                {(c.formats ?? []).map((f) => <Badge key={f} tone="accent">{FORMAT_LABEL[f] ?? f}</Badge>)}
                <Badge tone="neutral">CTA: {c.ctaType === 'BOOKING' ? 'Booking' : 'Link'} · {c.ctaLabel}</Badge>
              </div>
              {c.ctaType === 'BOOKING' ? (
                <p className="text-label text-muted">Bookable service: {c.serviceId ? <code className="rounded bg-sand px-1.5 py-0.5">{c.serviceId}</code> : 'not linked'}</p>
              ) : (
                <p className="text-label text-muted">
                  Destination: {safeHref(c.ctaUrl) ? <a href={safeHref(c.ctaUrl)!} target="_blank" rel="noreferrer" className="font-semibold text-teal hover:text-teal-dark">{c.ctaUrl}</a> : <code className="rounded bg-sand px-1.5 py-0.5">{c.ctaUrl ?? 'not set'}</code>}
                </p>
              )}
              {rejecting === c.id ? (
                <div className="space-y-2 rounded-2xl bg-danger-soft p-3">
                  <TextArea label="Rejection reason" value={reason} onChange={(e) => setReason(e.target.value)} rows={2} maxLength={500} placeholder="What should the partner fix?" />
                  <div className="flex gap-2">
                    <Button variant="danger" onClick={() => act(c, 'reject')} disabled={busy === `${c.id}reject`}>{busy === `${c.id}reject` ? 'Rejecting…' : 'Confirm rejection'}</Button>
                    <Button variant="ghost" onClick={() => { setRejecting(null); setReason(''); }}>Keep</Button>
                  </div>
                </div>
              ) : (
                <div className="flex flex-wrap gap-2">
                  <Button onClick={() => act(c, 'approve')} disabled={busy !== null}>{busy === `${c.id}approve` ? 'Approving…' : 'Approve'}</Button>
                  <Button variant="secondary" onClick={() => setRejecting(c.id ?? null)}>Reject</Button>
                  <Button variant="ghost" onClick={() => act(c, c.partnerVerified ? 'unverify' : 'verify')} disabled={busy !== null}>
                    {c.partnerVerified ? 'Unverify business' : 'Verify business'}
                  </Button>
                </div>
              )}
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
