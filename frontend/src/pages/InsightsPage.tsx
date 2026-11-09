import { useEffect, useState } from 'react';
import { ApiError, generateInsight, listPets } from '../api/client';
import { Card, EmptyState, ErrorMessage } from '../components/ui';
import type { Insight, Pet } from '../types';

export function InsightsPage() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [petId, setPetId] = useState('');
  const [periodDays, setPeriodDays] = useState('7');
  const [insight, setInsight] = useState<Insight | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    listPets()
      .then((petList) => {
        setPets(petList);
        if (petList[0]) {
          setPetId(petList[0].id);
        }
      })
      .catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load pets'));
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
    <div className="max-w-3xl space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">AI Activity Coach</h1>
        <p className="text-slate-600">Non-medical, personalized activity summaries based on recorded data.</p>
      </div>
      <Card title="Generate insight">
        <div className="grid gap-4 sm:grid-cols-[1fr_140px_auto] sm:items-end">
          <label className="block text-sm font-medium text-slate-700">Pet
            <select value={petId} onChange={(event) => setPetId(event.target.value)} className="mt-1 w-full rounded-xl border border-slate-300 px-3 py-2">
              <option value="">All pets</option>
              {pets.map((pet) => <option key={pet.id} value={pet.id}>{pet.name}</option>)}
            </select>
          </label>
          <label className="block text-sm font-medium text-slate-700">Period (days)
            <input type="number" min="1" max="90" value={periodDays} onChange={(event) => setPeriodDays(event.target.value)} className="mt-1 w-full rounded-xl border border-slate-300 px-3 py-2" />
          </label>
          <button type="button" onClick={handleGenerate} disabled={loading} className="rounded-xl bg-emerald-600 px-5 py-3 font-semibold text-white hover:bg-emerald-700 disabled:opacity-60">
            {loading ? 'Generating…' : 'Generate'}
          </button>
        </div>
        <ErrorMessage message={error} />
      </Card>
      {insight ? (
        <Card title="Latest insight">
          <p className="text-lg leading-7 text-slate-800">{insight.text}</p>
          <div className="mt-4 flex flex-wrap gap-2 text-xs">
            <span className="rounded-full bg-slate-100 px-3 py-1 text-slate-600">Provider: {insight.provider}</span>
            {insight.model ? <span className="rounded-full bg-slate-100 px-3 py-1 text-slate-600">Model: {insight.model}</span> : null}
            {insight.fallbackUsed ? <span className="rounded-full bg-amber-100 px-3 py-1 text-amber-700">Fallback response</span> : null}
          </div>
        </Card>
      ) : (
        <EmptyState title="No insight generated" description="Generate a summary after recording activities." />
      )}
    </div>
  );
}
