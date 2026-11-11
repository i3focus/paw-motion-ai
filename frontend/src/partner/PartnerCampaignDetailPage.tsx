import { useCallback, useEffect, useState } from 'react';
import { Link, useLocation, useParams, useSearchParams } from 'react-router-dom';
import { ApiError } from '../api/client';
import {
  endCampaign,
  getCampaign,
  getCampaignMetrics,
  pauseCampaign,
  resumeCampaign,
  submitCampaign,
  updateCampaign,
  type CampaignMetrics,
  type CampaignV2,
} from '../api/partner';
import { Badge, Button, Card, ErrorMessage, LoadingState, SectionHeader, StatCard } from '../components/ui';
import { CampaignForm, draftErrors, draftFromCampaign, draftToUpdate, useMediaUpload, type CampaignDraft } from './PartnerCampaignForm';
import { formatList, STATUS_TONE, statusLabel } from './PartnerCampaignsPage';

const EDITABLE = new Set(['DRAFT', 'PAUSED']);

export function PartnerCampaignDetailPage() {
  const { id = '' } = useParams();
  const location = useLocation();
  const [params, setParams] = useSearchParams();
  const [campaign, setCampaign] = useState<CampaignV2 | null>(null);
  const [metrics, setMetrics] = useState<CampaignMetrics | null>(null);
  const [draft, setDraft] = useState<CampaignDraft | null>(null);
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>((location.state as { warning?: string } | null)?.warning ?? null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState<string | null>(null);
  const [dirty, setDirty] = useState(false);
  const updateDraft = (d: CampaignDraft) => { setDraft(d); setDirty(true); };
  const { upload, uploading, uploadError } = useMediaUpload((patch) => setDraft((d) => (d ? { ...d, ...patch } : d)));

  const reload = useCallback(() => {
    getCampaign(id).then((c) => { setCampaign(c); setDraft(draftFromCampaign(c)); })
      .catch((e) => setError(e instanceof ApiError ? e.message : 'Unable to load campaign'));
    getCampaignMetrics(id).then(setMetrics).catch(() => setMetrics(null));
  }, [id]);

  useEffect(reload, [reload]);

  useEffect(() => {
    if (params.get('submit') === '1') {
      params.delete('submit');
      setParams(params, { replace: true });
      if (campaign?.status === 'DRAFT') doSubmit();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [campaign]);

  async function save(andSubmit: boolean) {
    if (!draft) return;
    const errs = draftErrors(draft);
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;
    setBusy('save');
    setError(null);
    try {
      const updated = await updateCampaign(id, draftToUpdate(draft, true));
      setCampaign(updated);
      setDraft(draftFromCampaign(updated));
      setDirty(false);
      if (andSubmit) {
        await doSubmit();
      } else {
        setNotice('Saved.');
      }
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to save');
      setNotice(null);
    } finally {
      setBusy(null);
    }
  }

  async function doSubmit() {
    setBusy('submit');
    setError(null);
    setNotice(null);
    try {
      const updated = await submitCampaign(id);
      setCampaign(updated);
      setDraft(draftFromCampaign(updated));
      setDirty(false);
      setNotice('Submitted for review — you will be notified once it is approved.');
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to submit');
    } finally {
      setBusy(null);
    }
  }

  async function lifecycle(action: 'pause' | 'resume' | 'end') {
    setBusy(action);
    setError(null);
    setNotice(null);
    try {
      const updated = await (action === 'pause' ? pauseCampaign(id) : action === 'resume' ? resumeCampaign(id) : endCampaign(id));
      setCampaign(updated);
      setDraft(draftFromCampaign(updated));
      if (action === 'end') setNotice('Campaign ended. Remaining budget was refunded.');
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to update status');
    } finally {
      setBusy(null);
    }
  }

  if (error && !campaign) {
    return <Card><ErrorMessage message={error} /><Link to="/partner/campaigns"><Button variant="ghost" className="mt-4">Back to campaigns</Button></Link></Card>;
  }
  if (!campaign || !draft) return <LoadingState label="Loading campaign…" />;

  const editable = EDITABLE.has(campaign.status ?? '');
  const m = metrics;

  return (
    <div className="space-y-6">
      <SectionHeader
        title={campaign.title ?? 'Campaign'}
        subtitle={`${formatList(campaign.formats)} · ${new Date(campaign.startsAt ?? '').toLocaleDateString()} → ${new Date(campaign.endsAt ?? '').toLocaleDateString()}`}
        action={<Badge tone={STATUS_TONE[campaign.status ?? ''] ?? 'neutral'}>{statusLabel(campaign)}</Badge>}
      />
      {campaign.rejectionReason ? (
        <p className="rounded-2xl bg-danger-soft px-4 py-3 text-body text-danger">
          Rejected by review: {campaign.rejectionReason} — fix the issues and resubmit.
        </p>
      ) : null}
      {notice ? <p className="rounded-2xl bg-success-soft px-4 py-3 text-body text-success">{notice}</p> : null}
      <ErrorMessage message={error ?? uploadError} />

      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        <StatCard label="Impressions" value={m?.impressions ?? '—'} hint="times the ad was shown" />
        <StatCard label="Clicks" value={m?.clicks ?? '—'} hint={m ? `CTR ${((m.clickThroughRate ?? 0) * 100).toFixed(1)}%` : undefined} />
        <StatCard label="Bookings" value={m?.bookings ?? '—'} hint={m ? `${((m.bookingRate ?? 0) * 100).toFixed(1)}% of impressions` : undefined} />
        <StatCard label="Credits spent" value={m?.creditsSpent ?? '—'} hint={m?.costPerBooking != null ? `${m.costPerBooking.toFixed(1)} per booking` : m?.costPerClick != null ? `${m.costPerClick.toFixed(1)} per click` : undefined} />
      </div>

      <Card title={editable ? 'Edit campaign' : 'Campaign details'} description={editable ? 'Changes are allowed while the campaign is a draft or paused.' : 'A campaign in review, active or ended cannot be edited — pause an active campaign to change details.'}>
        <fieldset disabled={!editable || busy !== null} className="disabled:opacity-70">
          <CampaignForm draft={draft} onChange={updateDraft} errors={errors} onMedia={upload} uploading={uploading} />
        </fieldset>
        {editable ? (
          <div className="mt-6 flex flex-wrap gap-3">
            <Button onClick={() => save(false)} disabled={busy !== null || uploading}>{busy === 'save' ? 'Saving…' : 'Save changes'}</Button>
            {campaign.status === 'DRAFT' ? (
              <Button variant="secondary" onClick={() => save(true)} disabled={busy !== null || uploading}>{busy === 'submit' ? 'Submitting…' : 'Save & submit for review'}</Button>
            ) : null}
          </div>
        ) : null}
      </Card>

      <Card title="Lifecycle" description="Review, delivery and budget controls.">
        <div className="flex flex-wrap items-center gap-3">
          {campaign.status === 'DRAFT' ? (
            <Button onClick={() => { if (dirty && !confirm('Submit discards unsaved edits — save them first using "Save & submit for review". Continue anyway?')) return; void doSubmit(); }} disabled={busy !== null}>
              {busy === 'submit' ? 'Submitting…' : 'Submit for review'}
            </Button>
          ) : null}
          {campaign.status === 'ACTIVE' ? <Button variant="secondary" onClick={() => lifecycle('pause')} disabled={busy !== null}>Pause delivery</Button> : null}
          {campaign.status === 'PAUSED' ? <Button variant="secondary" onClick={() => lifecycle('resume')} disabled={busy !== null}>Resume delivery</Button> : null}
          {campaign.status !== 'ENDED' ? (
            <Button variant="ghost" onClick={() => { if (confirm('End this campaign? Unused budget is refunded.')) lifecycle('end'); }} disabled={busy !== null}>End campaign</Button>
          ) : (
            <p className="text-body text-muted">Ended{campaign.endReason ? ` · ${campaign.endReason.toLowerCase().replace('_', ' ')}` : ''} · {campaign.creditsRefunded} credits refunded</p>
          )}
          <Link to="/partner/campaigns" className="ml-auto"><Button variant="ghost">← All campaigns</Button></Link>
        </div>
      </Card>
    </div>
  );
}
