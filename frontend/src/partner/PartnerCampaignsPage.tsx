import { useEffect, useRef, useState, type FormEvent } from 'react';
import { analyzeCampaign, ApiError, archiveCampaign, createCampaign, listCampaigns } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorMessage, SectionHeader, TextArea, TextInput } from '../components/ui';
import type { Campaign } from '../types';

function isExpired(campaign: Campaign): boolean {
  return campaign.status === 'EXPIRED' || new Date(campaign.expiresAt).getTime() <= Date.now();
}

function vigencyLabel(campaign: Campaign): string {
  const expiresAt = new Date(campaign.expiresAt);
  if (isExpired(campaign)) {
    return `expired ${expiresAt.toLocaleDateString()}`;
  }
  const days = Math.max(1, Math.ceil((expiresAt.getTime() - Date.now()) / 86_400_000));
  return `${days} day${days === 1 ? '' : 's'} left · expires ${expiresAt.toLocaleDateString()}`;
}

export function PartnerCampaignsPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [targetAudience, setTargetAudience] = useState('');
  const [callToAction, setCallToAction] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [analyzingId, setAnalyzingId] = useState<string | null>(null);
  const [archivingId, setArchivingId] = useState<string | null>(null);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const campaignKey = useRef<{ fingerprint: string; key: string } | null>(null);

  async function refresh() {
    setCampaigns(await listCampaigns());
  }

  useEffect(() => {
    refresh().catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load campaigns'));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errors: Record<string, string> = {};
    const trimmedTitle = title.trim();
    const trimmedDescription = description.trim();
    const trimmedAudience = targetAudience.trim();
    const trimmedCta = callToAction.trim();
    if (!trimmedTitle) {
      errors.title = 'Campaign title is required';
    } else if (trimmedTitle.length > 160) {
      errors.title = 'Title must be 160 characters or fewer';
    }
    if (!trimmedDescription) {
      errors.description = 'Description is required';
    } else if (trimmedDescription.length > 2000) {
      errors.description = 'Description must be 2000 characters or fewer';
    }
    if (!trimmedAudience) {
      errors.targetAudience = 'Target audience is required';
    } else if (trimmedAudience.length > 160) {
      errors.targetAudience = 'Target audience must be 160 characters or fewer';
    }
    if (!trimmedCta) {
      errors.callToAction = 'Call to action is required';
    } else if (trimmedCta.length > 160) {
      errors.callToAction = 'Call to action must be 160 characters or fewer';
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setError(null);
    setSubmitting(true);
    const fingerprint = [trimmedTitle, trimmedDescription, trimmedAudience, trimmedCta].join('|');
    if (!campaignKey.current || campaignKey.current.fingerprint !== fingerprint) {
      campaignKey.current = { fingerprint, key: crypto.randomUUID() };
    }
    try {
      await createCampaign({
        title: trimmedTitle,
        description: trimmedDescription,
        targetAudience: trimmedAudience,
        callToAction: trimmedCta,
      }, campaignKey.current.key);
      campaignKey.current = null;
      setTitle('');
      setDescription('');
      setTargetAudience('');
      setCallToAction('');
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to create campaign');
    } finally {
      setSubmitting(false);
    }
  }

  async function handleAnalyze(campaignId: string) {
    setError(null);
    setAnalyzingId(campaignId);
    try {
      await analyzeCampaign(campaignId);
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to analyze campaign');
    } finally {
      setAnalyzingId(null);
    }
  }

  async function handleArchive(campaignId: string) {
    setError(null);
    setArchivingId(campaignId);
    try {
      await archiveCampaign(campaignId);
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to archive campaign');
    } finally {
      setArchivingId(null);
    }
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="Campaigns" subtitle="Create advertisements and campaigns, then run AI analysis to generate owner-facing topics." />
      <div className="grid gap-6 lg:grid-cols-[420px_1fr]">
        <Card title="Create campaign" description="Clear campaign details help the AI recommend relevant topics.">
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <TextInput
              label="Campaign title"
              required
              value={title}
              onChange={(event) => setTitle(event.target.value)}
              error={fieldErrors.title}
              maxLength={160}
            />
            <TextArea
              label="Description"
              required
              value={description}
              onChange={(event) => setDescription(event.target.value)}
              error={fieldErrors.description}
              rows={4}
              maxLength={2000}
            />
            <TextInput
              label="Target audience"
              required
              value={targetAudience}
              onChange={(event) => setTargetAudience(event.target.value)}
              error={fieldErrors.targetAudience}
              maxLength={160}
              placeholder="Active dog owners"
            />
            <TextInput
              label="Call to action"
              required
              value={callToAction}
              onChange={(event) => setCallToAction(event.target.value)}
              error={fieldErrors.callToAction}
              maxLength={160}
              placeholder="Try the 7-day walk challenge"
            />
            <ErrorMessage message={error} />
            <Button type="submit" disabled={submitting} className="w-full">{submitting ? 'Saving…' : 'Create campaign'}</Button>
          </form>
        </Card>

        <Card title="Your campaigns" description="Analyzed campaigns feed owner recommendations.">
          {campaigns.length === 0 ? (
            <EmptyState title="No campaigns yet" description="Create a campaign and analyze it to recommend topics to pet owners." />
          ) : (
            <div className="space-y-4">
              {campaigns.map((campaign) => (
                <div key={campaign.id} className="rounded-3xl border border-cream-200 bg-cream-50 p-4">
                  <div className="flex flex-wrap items-start justify-between gap-3">
                    <div>
                      <div className="flex items-center gap-2">
                        <h3 className="font-semibold text-cream-900">{campaign.title}</h3>
                        <Badge tone={campaign.status === 'ACTIVE' ? 'success' : isExpired(campaign) ? 'danger' : 'neutral'}>{isExpired(campaign) ? 'EXPIRED' : campaign.status}</Badge>
                      </div>
                      <p className="mt-1 text-sm text-cream-500">Audience: {campaign.targetAudience}</p>
                      <p className="mt-1 text-xs text-cream-500">{campaign.creditsCharged} credits charged · {campaign.creditsRefunded} refunded · {vigencyLabel(campaign)}</p>
                    </div>
                    <div className="flex gap-2">
                      {campaign.status !== 'ARCHIVED' && campaign.status !== 'EXPIRED' ? (
                        <Button type="button" variant="secondary" onClick={() => handleAnalyze(campaign.id)} disabled={analyzingId === campaign.id}>
                          {analyzingId === campaign.id ? 'Analyzing…' : campaign.aiTopics.length ? 'Re-analyze' : 'Analyze with AI'}
                        </Button>
                      ) : null}
                      {campaign.status !== 'ARCHIVED' && campaign.status !== 'EXPIRED' ? (
                        <Button type="button" variant="ghost" onClick={() => handleArchive(campaign.id)} disabled={archivingId === campaign.id}>
                          {archivingId === campaign.id ? 'Archiving…' : 'Archive'}
                        </Button>
                      ) : null}
                    </div>
                  </div>
                  <p className="mt-3 text-sm text-cream-700">{campaign.description}</p>
                  {campaign.callToAction ? <p className="mt-2 text-sm font-medium text-primary">CTA: {campaign.callToAction}</p> : null}
                  {campaign.aiTopics.length ? (
                    <div className="mt-3">
                      <p className="text-xs font-semibold uppercase tracking-wide text-cream-500">Recommended owner topics</p>
                      <div className="mt-2 flex flex-wrap gap-2">
                        {campaign.aiTopics.map((topic) => <Badge key={topic} tone="accent">{topic}</Badge>)}
                      </div>
                    </div>
                  ) : (
                    <p className="mt-3 text-sm text-cream-500">Run AI analysis to activate owner recommendations.</p>
                  )}
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
