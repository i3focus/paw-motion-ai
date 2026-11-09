import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, createActivity, listActivities, listPets } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorMessage, Field, SectionHeader, SelectInput, TextArea, TextInput } from '../components/ui';
import type { Activity, Pet } from '../types';

const activityTypes = ['walk', 'run', 'hike', 'bike_ride', 'swim', 'play'];

function toLocalInputValue(date: Date): string {
  const offset = date.getTimezoneOffset() * 60000;
  return new Date(date.getTime() - offset).toISOString().slice(0, 16);
}

export function ActivitiesPage() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [petId, setPetId] = useState('');
  const [type, setType] = useState('walk');
  const [startedAt, setStartedAt] = useState(() => toLocalInputValue(new Date()));
  const [durationMinutes, setDurationMinutes] = useState('30');
  const [distanceKm, setDistanceKm] = useState('');
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);

  async function refresh() {
    const [petList, activityList] = await Promise.all([listPets(), listActivities()]);
    setPets(petList);
    setActivities(activityList);
    if (!petId && petList[0]) {
      setPetId(petList[0].id);
    }
  }

  useEffect(() => {
    refresh().catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load activities'));
  }, []);

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await createActivity({
        petId,
        type,
        startedAt: new Date(startedAt).toISOString(),
        durationSeconds: Math.round(Number(durationMinutes) * 60),
        distanceMeters: distanceKm ? Math.round(Number(distanceKm) * 1000) : undefined,
        notes: notes || undefined,
      });
      setNotes('');
      setDistanceKm('');
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to record activity');
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="Activities" subtitle="Record walks, runs, play sessions, and outdoor time." />
      <div className="grid gap-6 lg:grid-cols-[380px_1fr]">
        <Card title="Record activity">
          <form onSubmit={handleSubmit} className="space-y-4">
            <Field label="Pet">
              <SelectInput value={petId} onChange={(event) => setPetId(event.target.value)} required>
                {pets.length === 0 ? <option value="">Create a pet first</option> : pets.map((pet) => <option key={pet.id} value={pet.id}>{pet.name}</option>)}
              </SelectInput>
            </Field>
            <Field label="Activity type">
              <SelectInput value={type} onChange={(event) => setType(event.target.value)}>
                {activityTypes.map((option) => <option key={option} value={option}>{option.replace('_', ' ')}</option>)}
              </SelectInput>
            </Field>
            <Field label="Started at"><TextInput type="datetime-local" value={startedAt} onChange={(event) => setStartedAt(event.target.value)} required /></Field>
            <div className="grid grid-cols-2 gap-3">
              <Field label="Duration (min)"><TextInput type="number" min="1" value={durationMinutes} onChange={(event) => setDurationMinutes(event.target.value)} required /></Field>
              <Field label="Distance (km)"><TextInput type="number" min="0" step="0.01" value={distanceKm} onChange={(event) => setDistanceKm(event.target.value)} /></Field>
            </div>
            <Field label="Notes"><TextArea value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} maxLength={500} /></Field>
            <ErrorMessage message={error} />
            <Button type="submit" disabled={submitting || pets.length === 0} className="w-full">{submitting ? 'Saving…' : 'Record activity'}</Button>
          </form>
        </Card>

        <Card title="Activity history">
          {activities.length === 0 ? (
            <EmptyState title="No activities recorded" description="Use the form to record a walk, run, hike, ride, swim, or play session." />
          ) : (
            <div className="space-y-3">
              {activities.map((activity) => {
                const pet = pets.find((candidate) => candidate.id === activity.petId);
                return (
                  <div key={activity.id} className="rounded-3xl border border-cream-200 bg-cream-50 p-4">
                    <div className="flex items-center justify-between gap-4">
                      <div>
                        <div className="flex items-center gap-2">
                          <p className="font-semibold capitalize text-cream-900">{activity.type.replace('_', ' ')}</p>
                          <Badge>{pet?.name ?? 'Pet'}</Badge>
                        </div>
                        <p className="mt-1 text-sm text-cream-500">{new Date(activity.startedAt).toLocaleString()}</p>
                      </div>
                      <div className="text-right text-sm text-cream-600">
                        <p className="font-semibold">{Math.round(activity.durationSeconds / 60)} min</p>
                        <p>{activity.distanceMeters ? `${(activity.distanceMeters / 1000).toFixed(2)} km` : 'No distance'}</p>
                      </div>
                    </div>
                    {activity.notes ? <p className="mt-3 text-sm text-cream-600">{activity.notes}</p> : null}
                  </div>
                );
              })}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
