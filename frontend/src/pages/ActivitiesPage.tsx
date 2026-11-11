import { useEffect, useState, type FormEvent } from 'react';
import { ApiError, createActivity, listActivities, listPets } from '../api/client';
import { ActivityCard } from '../components/petVisuals';
import { Button, Card, EmptyState, ErrorMessage, NumberInput, SectionHeader, SelectInput, TextArea, TextInput } from '../components/ui';
import { isNonNegativeNumber, isPositiveInteger } from '../lib/validation';
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
  const [latitude, setLatitude] = useState('');
  const [longitude, setLongitude] = useState('');
  const [locating, setLocating] = useState(false);
  const [shareToPawCity, setShareToPawCity] = useState(false);
  const [notes, setNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

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

  function useCurrentLocation() {
    if (!navigator.geolocation) {
      setError('Geolocation is not available in this browser');
      return;
    }
    setLocating(true);
    setError(null);
    navigator.geolocation.getCurrentPosition(
      (position) => {
        setLatitude(position.coords.latitude.toFixed(6));
        setLongitude(position.coords.longitude.toFixed(6));
        setLocating(false);
      },
      () => {
        setError('Unable to read your location');
        setLocating(false);
      },
      { enableHighAccuracy: true, timeout: 10000 },
    );
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errors: Record<string, string> = {};
    if (!petId) {
      errors.petId = 'Select a pet';
    }
    if (!startedAt || Number.isNaN(new Date(startedAt).getTime())) {
      errors.startedAt = 'Enter a valid start date and time';
    }
    if (!isPositiveInteger(durationMinutes)) {
      errors.durationMinutes = 'Duration must be a positive whole number of minutes';
    }
    if (distanceKm && !isNonNegativeNumber(distanceKm)) {
      errors.distanceKm = 'Distance must be zero or greater';
    }
    if (latitude && (Number(latitude) < -90 || Number(latitude) > 90)) {
      errors.latitude = 'Latitude must be between -90 and 90';
    }
    if (longitude && (Number(longitude) < -180 || Number(longitude) > 180)) {
      errors.longitude = 'Longitude must be between -180 and 180';
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setError(null);
    setSubmitting(true);
    try {
      await createActivity({
        petId,
        type,
        startedAt: new Date(startedAt).toISOString(),
        durationSeconds: Math.round(Number(durationMinutes) * 60),
        distanceMeters: distanceKm ? Math.round(Number(distanceKm) * 1000) : undefined,
        latitude: latitude ? Number(latitude) : undefined,
        longitude: longitude ? Number(longitude) : undefined,
        shareToPawCity,
        notes: notes.trim() || undefined,
      });
      setNotes('');
      setDistanceKm('');
      setLatitude('');
      setLongitude('');
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
      <div className="grid gap-6 lg:grid-cols-[420px_1fr]">
        <Card title="Record activity" description="Use real activity data to power dashboard insights.">
          <form onSubmit={handleSubmit} className="space-y-4" noValidate>
            <SelectInput
              label="Pet"
              required
              value={petId}
              onChange={(event) => setPetId(event.target.value)}
              error={fieldErrors.petId}
            >
              {pets.length === 0 ? <option value="">Create a pet first</option> : pets.map((pet) => <option key={pet.id} value={pet.id}>{pet.name}</option>)}
            </SelectInput>
            <SelectInput label="Activity type" value={type} onChange={(event) => setType(event.target.value)}>
              {activityTypes.map((option) => <option key={option} value={option}>{option.replace('_', ' ')}</option>)}
            </SelectInput>
            <TextInput
              label="Started at"
              type="datetime-local"
              required
              value={startedAt}
              onChange={(event) => setStartedAt(event.target.value)}
              error={fieldErrors.startedAt}
            />
            <div className="grid grid-cols-2 gap-3">
              <NumberInput
                label="Duration (min)"
                required
                min="1"
                step="1"
                value={durationMinutes}
                onChange={(event) => setDurationMinutes(event.target.value)}
                error={fieldErrors.durationMinutes}
              />
              <NumberInput
                label="Distance (km)"
                min="0"
                step="0.01"
                value={distanceKm}
                onChange={(event) => setDistanceKm(event.target.value)}
                error={fieldErrors.distanceKm}
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <NumberInput
                label="Latitude"
                min="-90"
                max="90"
                step="0.000001"
                value={latitude}
                onChange={(event) => setLatitude(event.target.value)}
                error={fieldErrors.latitude}
                placeholder="Optional"
              />
              <NumberInput
                label="Longitude"
                min="-180"
                max="180"
                step="0.000001"
                value={longitude}
                onChange={(event) => setLongitude(event.target.value)}
                error={fieldErrors.longitude}
                placeholder="Optional"
              />
            </div>
            <Button type="button" variant="secondary" onClick={useCurrentLocation} disabled={locating} className="w-full">
              {locating ? 'Reading location…' : 'Use current location'}
            </Button>
            <label className="flex items-start gap-3 rounded-2xl border border-cream-200 bg-cream-50 p-3 text-sm">
              <input
                type="checkbox"
                checked={shareToPawCity}
                onChange={(event) => setShareToPawCity(event.target.checked)}
                className="mt-1 h-4 w-4 accent-primary"
              />
              <span>
                <span className="font-semibold text-cream-900">Share to Paw City</span>
                <span className="block text-cream-500">Show this activity as a post with reactions and comments.</span>
              </span>
            </label>
            <TextArea label="Notes" value={notes} onChange={(event) => setNotes(event.target.value)} rows={3} maxLength={500} />
            <ErrorMessage message={error} />
            <Button type="submit" disabled={submitting || pets.length === 0} className="w-full">{submitting ? 'Saving…' : 'Record activity'}</Button>
          </form>
        </Card>

        <Card title="Activity history" description="Recent recorded sessions with duration and distance.">
          {activities.length === 0 ? (
            <EmptyState title="No activities recorded" description="Use the form to record a walk, run, hike, ride, swim, or play session." />
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              {activities.map((activity) => {
                const pet = pets.find((candidate) => candidate.id === activity.petId);
                return <ActivityCard key={activity.id} activity={activity} petName={pet?.name} />;
              })}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
