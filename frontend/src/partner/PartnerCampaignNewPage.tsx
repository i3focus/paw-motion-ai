import { useRef, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { ApiError, createCampaign } from '../api/client';
import { updateCampaign } from '../api/partner';
import { Button, Card, ErrorMessage, SectionHeader } from '../components/ui';
import { CampaignForm, draftErrors, draftFromCampaign, draftToUpdate, useMediaUpload, type CampaignDraft } from './PartnerCampaignForm';

export function PartnerCampaignNewPage() {
  const navigate = useNavigate();
  const [draft, setDraft] = useState<CampaignDraft>(draftFromCampaign());
  const [errors, setErrors] = useState<Record<string, string>>({});
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const idempotencyKey = useRef(crypto.randomUUID());
  const { upload, uploading, uploadError } = useMediaUpload((patch) => setDraft((d) => ({ ...d, ...patch })));

  async function save(andSubmit: boolean) {
    const errs = draftErrors(draft);
    setErrors(errs);
    if (Object.keys(errs).length > 0) return;
    setSubmitting(true);
    setError(null);
    try {
      const created = await createCampaign({
        title: draft.title.trim(),
        description: draft.description.trim(),
        targetAudience: draft.targetAudience.trim(),
        callToAction: draft.ctaLabel.trim(),
      }, idempotencyKey.current);
      if (!created.id) throw new Error('Campaign creation failed');
      try {
        await updateCampaign(created.id, draftToUpdate(draft, false));
      } catch {
        navigate(`/partner/campaigns/${created.id}`, {
          replace: true,
          state: { warning: 'Draft created, but some fields could not be saved — review the campaign and save again.' },
        });
        return;
      }
      navigate(andSubmit ? `/partner/campaigns/${created.id}?submit=1` : `/partner/campaigns/${created.id}`, { replace: true });
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to create campaign');
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="New campaign" subtitle="Set up the ad, targeting and formats — you can submit it for review right away or keep it as a draft." />
      <Card>
        <CampaignForm draft={draft} onChange={setDraft} errors={errors} onMedia={upload} uploading={uploading} />
        <ErrorMessage message={uploadError ?? error} />
        <div className="mt-6 flex flex-wrap gap-3">
          <Button onClick={() => save(false)} disabled={submitting || uploading}>{submitting ? 'Saving…' : 'Save draft'}</Button>
          <Button variant="secondary" onClick={() => save(true)} disabled={submitting || uploading}>Save & submit for review</Button>
          <Link to="/partner/campaigns"><Button variant="ghost">Cancel</Button></Link>
        </div>
      </Card>
    </div>
  );
}
