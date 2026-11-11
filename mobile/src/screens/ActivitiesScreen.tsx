import { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, createActivity, listActivities, listPets } from '../api/client';
import { Button, Card, EmptyState, ErrorText, Field, Input } from '../components';
import { ActivityCard } from '../petVisuals';
import type { Activity, Pet } from '../types';
import { colors, fonts } from '../theme';
import { isNonNegativeNumber, isPositiveInteger } from '../validation';

const activityTypes = ['walk', 'run', 'hike', 'bike_ride', 'swim', 'play'];

export function ActivitiesScreen() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [activities, setActivities] = useState<Activity[]>([]);
  const [petId, setPetId] = useState('');
  const [type, setType] = useState('walk');
  const [startedAt, setStartedAt] = useState(() => new Date().toISOString());
  const [durationMinutes, setDurationMinutes] = useState('30');
  const [distanceKm, setDistanceKm] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    const [petList, activityList] = await Promise.all([listPets(), listActivities()]);
    setPets(petList);
    setActivities(activityList);
    if (!petId && petList[0]) {
      setPetId(petList[0].id);
    }
  }, [petId]);

  useEffect(() => {
    load().catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load activities'));
  }, [load]);

  async function submit() {
    const errors: string[] = [];
    if (!petId) {
      errors.push('Select a pet');
    }
    if (!activityTypes.includes(type)) {
      errors.push(`Activity type must be one of: ${activityTypes.join(', ')}`);
    }
    if (!startedAt || Number.isNaN(new Date(startedAt).getTime())) {
      errors.push('Enter a valid ISO start date/time');
    }
    if (!isPositiveInteger(durationMinutes)) {
      errors.push('Duration must be a positive whole number of minutes');
    }
    if (distanceKm && !isNonNegativeNumber(distanceKm)) {
      errors.push('Distance must be zero or greater');
    }
    if (errors.length) {
      setError(errors.join(' · '));
      return;
    }

    setError(null);
    setLoading(true);
    try {
      await createActivity({
        petId,
        type,
        startedAt: new Date(startedAt).toISOString(),
        durationSeconds: Math.round(Number(durationMinutes) * 60),
        distanceMeters: distanceKm ? Math.round(Number(distanceKm) * 1000) : undefined,
        notes: notes.trim() || undefined,
      });
      setNotes('');
      setDistanceKm('');
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to record activity');
    } finally {
      setLoading(false);
    }
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Activities</Text>
      <Card title="Record activity">
        <Field label="Pet">
          {pets.length ? (
            <View style={styles.petPicker}>
              {pets.map((pet) => (
                <Button key={pet.id} label={pet.name} variant={petId === pet.id ? 'primary' : 'secondary'} onPress={() => setPetId(pet.id)} />
              ))}
            </View>
          ) : (
            <ErrorText message="Create a pet profile before recording activities." />
          )}
        </Field>
        <Field label="Activity type"><Input value={type} onChangeText={setType} placeholder={activityTypes.join(', ')} autoCapitalize="none" /></Field>
        <Field label="Started at (ISO)"><Input value={startedAt} onChangeText={setStartedAt} autoCapitalize="none" /></Field>
        <Field label="Duration (minutes)"><Input value={durationMinutes} onChangeText={setDurationMinutes} keyboardType="numeric" /></Field>
        <Field label="Distance (km)"><Input value={distanceKm} onChangeText={setDistanceKm} keyboardType="decimal-pad" /></Field>
        <Field label="Notes"><Input value={notes} onChangeText={setNotes} multiline /></Field>
        <ErrorText message={error} />
        <Button label={loading ? 'Saving…' : 'Record activity'} onPress={submit} disabled={loading || !petId} />
      </Card>
      <Card title="History">
        {activities.length ? (
          <View style={styles.history}>
            {activities.map((activity) => {
              const pet = pets.find((candidate) => candidate.id === activity.petId);
              return <ActivityCard key={activity.id} activity={activity} petName={pet?.name} />;
            })}
          </View>
        ) : <EmptyState title="No activities recorded" description="Use the form to record a walk, run, hike, ride, swim, or play session." />}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  content: { padding: 20, gap: 16 },
  title: { color: colors.ink, fontSize: 30, fontFamily: fonts.w900 },
  petPicker: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  history: { gap: 12 },
});
