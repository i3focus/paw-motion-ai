import { useEffect, useState } from 'react';
import { ApiError, generateInsight, listPets, listRecommendations } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorMessage, Field, SectionHeader, SelectInput, TextInput } from '../components/ui';
import type { Insight, OwnerRecommendation, Pet } from '../types';

export function InsightsPage() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [petId, setPetId] = useState('');
  const [periodDays, setPeriodDays] = useState('7');
  const [insight, setInsight] = useState<Insight | null>(null);
  const [recommendations, setRecommendations] = useState<OwnerRecommendation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    Promise.all([
      listPets().then((petList) => {
        setPets(petList);
        if (petList[0]) {
          setPetId(petList[0].id);
        }
      }),
      listRecommendations().then(setRecommendations).catch(() => setRecommendations([])),
    ]).catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load insights'));
  }, []);

  async function handleGenerate() {
    setError(null);
    setLoading(true);
    try {
      setInsight(await generateInsight({ petId: petId || undefined, periodDays: Number(periodDays) }));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to generate insight');
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="AI Activity Coach" subtitle="Non-medical, personalized activity summaries and partner recommendations." />
      <div className="grid gap-6 lg:grid-cols-[1fr_420px]">
        <div className="space-y-6">
          <Card title="Generate insight">
            <div className="grid gap-4 sm:grid-cols-[1fr_140px_auto] sm:items-end">
              <Field label="Pet">
                <SelectInput value={petId} onChange={(event) => setPetId(event.target.value)}>
                  <option value="">All pets</option>
                  {pets.map((pet) => <option key={pet.id} value={pet.id}>{pet.name}</option>)}
                </SelectInput>
              </Field>
              <Field label="Period (days)">
                <TextInput type="number" min="1" max="90" value={periodDays} onChange={(event) => setPeriodDays(event.target.value)} />
              </Field>
              <Button type="button" onClick={handleGenerate} disabled={loading}>{loading ? 'Generating…' : 'Generate'}</Button>
            </div>
            <div className="mt-4"><ErrorMessage message={error} /></div>
          </Card>

          {insight ? (
            <Card title="Latest insight">
              <p className="text-lg leading-7 text-cream-800">{insight.text}</p>
              <div className="mt-4 flex flex-wrap gap-2">
                <Badge>Provider: {insight.provider}</Badge>
                {insight.model ? <Badge>Model: {insight.model}</Badge> : null}
                {insight.fallbackUsed ? <Badge tone="warning">Fallback response</Badge> : null}
              </div>
            </Card>
          ) : (
            <EmptyState title="No insight generated" description="Generate a summary after recording activities." />
          )}
        </div>

        <Card title="Partner recommendations">
          {recommendations.length === 0 ? (
            <EmptyState title="No recommendations yet" description="Partners can publish campaigns; AI-analyzed topics will appear here for pet owners." />
          ) : (
            <div className="space-y-3">
              {recommendations.map((recommendation) => (
                <div key={recommendation.campaignId} className="rounded-3xl border border-cream-200 bg-cream-50 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <p className="font-semibold text-cream-900">{recommendation.campaignTitle}</p>
                      <p className="mt-1 text-sm text-cream-500">{recommendation.partnerName}</p>
                    </div>
                    <Badge tone="accent">AI topics</Badge>
                  </div>
                  <p className="mt-3 text-sm text-cream-700">{recommendation.description}</p>
                  {recommendation.callToAction ? <p className="mt-2 text-sm font-medium text-primary">{recommendation.callToAction}</p> : null}
                  <div className="mt-3 flex flex-wrap gap-2">
                    {recommendation.recommendedTopics.map((topic) => <Badge key={topic} tone="neutral">{topic}</Badge>)}
                  </div>
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
