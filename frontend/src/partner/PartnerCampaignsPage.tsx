import { useEffect, useState, type FormEvent } from 'react';
import { analyzeCampaign, ApiError, createCampaign, listCampaigns } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorMessage, Field, SectionHeader, TextArea, TextInput } from '../components/ui';
import type { Campaign } from '../types';

export function PartnerCampaignsPage() {
  const [campaigns, setCampaigns] = useState<Campaign[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [targetAudience, setTargetAudience] = useState('');
  const [callToAction, setCallToAction] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [analyzingId, setAnalyzingId] = useState<string | null>(null);

  async function refresh() {
    setCampaigns(await listCampaigns());
  }

  useEffect(() => {
    refresh().catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load campaigns'));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createCampaign({ title, description, targetAudience, callToAction });
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

  return (
    <div className="space-y-6">
      <SectionHeader title="Campaigns" subtitle="Create advertisements and campaigns, then run AI analysis to generate owner-facing topics." />
      <div className="grid gap-6 lg:grid-cols-[420px_1fr]">
        <Card title="Create campaign">
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Campaign title"><TextInput value={title} onChange={(event) => setTitle(event.target.value)} required maxLength={160} /></Field>
            <Field label="Description"><TextArea value={description} onChange={(event) => setDescription(event.target.value)} required rows={4} maxLength={2000} /></Field>
            <Field label="Target audience"><TextInput value={targetAudience} onChange={(event) => setTargetAudience(event.target.value)} required maxLength={160} placeholder="Active dog owners" /></Field>
            <Field label="Call to action"><TextInput value={callToAction} onChange={(event) => setCallToAction(event.target.value)} required maxLength={160} placeholder="Try the 7-day walk challenge" /></Field>
            <ErrorMessage message={error} />
            <Button type="submit" disabled={submitting} className="w-full">{submitting ? 'Saving…' : 'Create campaign'}</Button>
          </form>
        </Card>

        <Card title="Your campaigns">
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
                        <Badge tone={campaign.status === 'ACTIVE' ? 'success' : 'neutral'}>{campaign.status}</Badge>
                      </div>
                      <p className="mt-1 text-sm text-cream-500">Audience: {campaign.targetAudience}</p>
                    </div>
                    <Button type="button" variant="secondary" onClick={() => handleAnalyze(campaign.id)} disabled={analyzingId === campaign.id}>
                      {analyzingId === campaign.id ? 'Analyzing…' : campaign.aiTopics.length ? 'Re-analyze' : 'Analyze with AI'}
                    </Button>
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
