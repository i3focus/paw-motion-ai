import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, createPet, listPets } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorMessage, Field, SectionHeader, SelectInput, TextInput } from '../components/ui';
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
    <div className="space-y-6">
      <SectionHeader title="Pets" subtitle="Create pet profiles and keep their details up to date." />
      <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
        <Card title="Create pet profile">
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Name"><TextInput value={name} onChange={(event) => setName(event.target.value)} required /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Species"><SelectInput value={species} onChange={(event) => setSpecies(event.target.value)}>{speciesOptions.map((option) => <option key={option} value={option}>{option}</option>)}</SelectInput></Field>
              <Field label="Size"><SelectInput value={size} onChange={(event) => setSize(event.target.value)}>{sizeOptions.map((option) => <option key={option} value={option}>{option}</option>)}</SelectInput></Field>
            </div>
            <Field label="Breed"><TextInput value={breed} onChange={(event) => setBreed(event.target.value)} /></Field>
            <Field label="Birth date"><TextInput type="date" value={birthDate} onChange={(event) => setBirthDate(event.target.value)} /></Field>
            <Field label="Activity preference"><TextInput value={activityPreference} onChange={(event) => setActivityPreference(event.target.value)} placeholder="Daily walks" /></Field>
            <ErrorMessage message={error} />
            <Button type="submit" disabled={submitting} className="w-full">{submitting ? 'Saving…' : 'Add pet'}</Button>
          </form>
        </Card>

        <Card title="Your pets">
          {pets.length === 0 ? (
            <EmptyState title="No pets yet" description="Add your first pet profile to start recording activities." />
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              {pets.map((pet) => (
                <div key={pet.id} className="rounded-3xl border border-cream-200 bg-cream-50 p-4">
                  <div className="flex items-start justify-between gap-3">
                    <div>
                      <h3 className="text-lg font-semibold capitalize text-cream-900">{pet.name}</h3>
                      <p className="text-sm capitalize text-cream-500">{pet.species} · {pet.size}</p>
                    </div>
                    <Badge tone="accent">{pet.status}</Badge>
                  </div>
                  {pet.breed ? <p className="mt-3 text-sm text-cream-600">{pet.breed}</p> : null}
                  {pet.activityPreference ? <p className="mt-1 text-sm text-cream-600">Preference: {pet.activityPreference}</p> : null}
                </div>
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
