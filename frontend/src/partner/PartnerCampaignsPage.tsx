import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { ApiError } from '../api/client';
import { listCampaignsV2, type CampaignV2 } from '../api/partner';
import { Badge, Button, Card, EmptyState, ErrorMessage, LoadingState, SectionHeader } from '../components/ui';

export const STATUS_TONE: Record<string, 'neutral' | 'accent' | 'success' | 'warning' | 'danger'> = {
  DRAFT: 'neutral',
  IN_REVIEW: 'warning',
  ACTIVE: 'success',
  PAUSED: 'accent',
  ENDED: 'neutral',
};

export const STATUS_LABEL: Record<string, string> = {
  DRAFT: 'Draft',
  IN_REVIEW: 'In review',
  ACTIVE: 'Active',
  PAUSED: 'Paused',
  ENDED: 'Ended',
};

export const FORMAT_LABEL: Record<string, string> = {
  FEED_CARD: 'Sponsored feed card',
  MAP_PIN: 'Promoted map pin',
  DISCOVER_HIGHLIGHT: 'Discover highlight',
  NEARBY_PROMO: '"Near you" promotion',
  AI_RECOMMENDATION: 'AI recommendation',
};

export function statusLabel(c: CampaignV2): string {
  if (c.status === 'DRAFT' && c.rejectionReason) return 'Rejected';
  return STATUS_LABEL[c.status ?? ''] ?? c.status ?? 'Unknown';
}

export function formatList(ids?: string[] | null): string {
  return (ids ?? []).map((f) => FORMAT_LABEL[f] ?? f).join(', ');
}

export function PartnerCampaignsPage() {
  const [campaigns, setCampaigns] = useState<CampaignV2[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    listCampaignsV2()
      .then(setCampaigns)
      .catch((caught) => { setError(caught instanceof ApiError ? caught.message : 'Unable to load campaigns'); setCampaigns([]); });
  }, []);

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Campaigns"
        subtitle="Create ads, send them for review and follow their performance inside PetLife."
        action={<Link to="/partner/campaigns/new"><Button>New campaign</Button></Link>}
      />
      <ErrorMessage message={error} />
      {campaigns === null ? <LoadingState label="Loading campaigns…" /> : campaigns.length === 0 ? (
        <Card>
          <EmptyState
            title="No campaigns yet"
            description="Create your first sponsored card, promoted pin or Discover highlight — it goes live after a quick review."
            action={<Link to="/partner/campaigns/new"><Button>Create a campaign</Button></Link>}
          />
        </Card>
      ) : (
        <div className="grid gap-4 md:grid-cols-2">
          {campaigns.map((c) => (
            <Card key={c.id} className="flex flex-col gap-3">
              <div className="flex items-start justify-between gap-3">
                <div className="min-w-0">
                  <h3 className="text-lg font-bold text-ink">{c.title}</h3>
                  <p className="text-label text-muted">
                    {new Date(c.startsAt ?? '').toLocaleDateString()} → {new Date(c.endsAt ?? '').toLocaleDateString()}
                  </p>
                </div>
                <Badge tone={STATUS_TONE[c.status ?? ''] ?? 'neutral'}>{statusLabel(c)}</Badge>
              </div>
              {c.mediaUrl ? <img src={c.mediaUrl} alt="" className="h-32 w-full rounded-2xl object-cover" /> : null}
              <p className="text-body text-muted line-clamp-2">{c.description}</p>
              <div className="flex flex-wrap gap-1.5">
                {(c.formats ?? []).map((f) => <Badge key={f} tone="accent">{FORMAT_LABEL[f] ?? f}</Badge>)}
              </div>
              {c.rejectionReason ? (
                <p className="rounded-2xl bg-coral-soft px-3 py-2 text-label text-coral">Rejected: {c.rejectionReason}</p>
              ) : null}
              <p className="text-label text-muted">
                CTA: {c.ctaType === 'BOOKING' ? `Book a service · ${c.ctaLabel}` : `${c.ctaLabel} → link`} ·{' '}
                {c.budgetCredits} credits budget · {c.creditsCharged} charged
              </p>
              <div className="mt-auto pt-1">
                <Link to={`/partner/campaigns/${c.id}`}>
                  <Button variant="secondary" className="w-full">Manage & metrics</Button>
                </Link>
              </div>
            </Card>
          ))}
        </div>
      )}
    </div>
  );
}
