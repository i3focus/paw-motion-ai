import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, createPet, listPets } from '../api/client';
import { Card, EmptyState, ErrorMessage } from '../components/ui';
import type { Pet } from '../types';

const speciesOptions = ['dog', 'cat', 'bird', 'rabbit', 'other'];
const sizeOptions = ['small', 'medium', 'large'];

export function PetsPage() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [species, setSpecies] = useState('dog');
  const [size, setSize] = useState('medium');
  const [breed, setBreed] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [activityPreference, setActivityPreference] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function refresh() {
    setPets(await listPets());
  }

  useEffect(() => {
    refresh().catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load pets'));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createPet({
        name,
        species,
        size,
        breed: breed || undefined,
        birthDate: birthDate || undefined,
        activityPreference: activityPreference || undefined,
      });
      setName('');
      setBreed('');
      setBirthDate('');
      setActivityPreference('');
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to create pet');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
      <Card title="Create pet profile">
        <form onSubmit={handleSubmit} className="space-y-4">
          <label className="block text-sm font-medium text-cream-700">Name<input value={name} onChange={(event) => setName(event.target.value)} required className="mt-1 w-full rounded-xl border border-cream-300 px-3 py-2" /></label>
          <div className="grid grid-cols-2 gap-3">
            <label className="block text-sm font-medium text-cream-700">Species<select value={species} onChange={(event) => setSpecies(event.target.value)} className="mt-1 w-full rounded-xl border border-cream-300 px-3 py-2">{speciesOptions.map((option) => <option key={option} value={option}>{option}</option>)}</select></label>
            <label className="block text-sm font-medium text-cream-700">Size<select value={size} onChange={(event) => setSize(event.target.value)} className="mt-1 w-full rounded-xl border border-cream-300 px-3 py-2">{sizeOptions.map((option) => <option key={option} value={option}>{option}</option>)}</select></label>
          </div>
          <label className="block text-sm font-medium text-cream-700">Breed<input value={breed} onChange={(event) => setBreed(event.target.value)} className="mt-1 w-full rounded-xl border border-cream-300 px-3 py-2" /></label>
          <label className="block text-sm font-medium text-cream-700">Birth date<input type="date" value={birthDate} onChange={(event) => setBirthDate(event.target.value)} className="mt-1 w-full rounded-xl border border-cream-300 px-3 py-2" /></label>
          <label className="block text-sm font-medium text-cream-700">Activity preference<input value={activityPreference} onChange={(event) => setActivityPreference(event.target.value)} placeholder="Daily walks" className="mt-1 w-full rounded-xl border border-cream-300 px-3 py-2" /></label>
          <ErrorMessage message={error} />
          <button type="submit" disabled={submitting} className="w-full rounded-xl bg-primary px-4 py-3 font-semibold text-white hover:bg-secondary disabled:opacity-60">{submitting ? 'Saving…' : 'Add pet'}</button>
        </form>
      </Card>

      <Card title="Your pets">
        {pets.length === 0 ? (
          <EmptyState title="No pets yet" description="Add your first pet profile to start recording activities." />
        ) : (
          <div className="grid gap-4 sm:grid-cols-2">
            {pets.map((pet) => (
              <div key={pet.id} className="rounded-2xl border border-cream-200 bg-cream-50 p-4">
                <div className="flex items-start justify-between">
                  <div>
                    <h3 className="text-lg font-semibold capitalize text-cream-900">{pet.name}</h3>
                    <p className="text-sm capitalize text-cream-500">{pet.species} · {pet.size}</p>
                  </div>
                  <span className="rounded-full bg-accent-soft px-3 py-1 text-xs font-medium text-secondary">{pet.status}</span>
                </div>
                {pet.breed ? <p className="mt-3 text-sm text-cream-600">{pet.breed}</p> : null}
                {pet.activityPreference ? <p className="mt-1 text-sm text-cream-600">Preference: {pet.activityPreference}</p> : null}
              </div>
            ))}
          </div>
        )}
      </Card>
    </div>
  );
}
