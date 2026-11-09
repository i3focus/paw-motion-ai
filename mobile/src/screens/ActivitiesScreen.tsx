import { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, createActivity, listActivities, listPets } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorText, Field, Input } from '../components';
import type { Activity, Pet } from '../types';
import { colors } from '../theme';

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
    setError(null);
    setLoading(true);
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
        <Field label="Activity type"><Input value={type} onChangeText={setType} placeholder={activityTypes.join(', ')} /></Field>
        <Field label="Started at (ISO)"><Input value={startedAt} onChangeText={setStartedAt} autoCapitalize="none" /></Field>
        <Field label="Duration (minutes)"><Input value={durationMinutes} onChangeText={setDurationMinutes} keyboardType="numeric" /></Field>
        <Field label="Distance (km)"><Input value={distanceKm} onChangeText={setDistanceKm} keyboardType="decimal-pad" /></Field>
        <Field label="Notes"><Input value={notes} onChangeText={setNotes} multiline /></Field>
        <ErrorText message={error} />
        <Button label={loading ? 'Saving…' : 'Record activity'} onPress={submit} disabled={loading || !petId || !type || !startedAt} />
      </Card>
      <Card title="History">
        {activities.length ? activities.map((activity) => {
          const pet = pets.find((candidate) => candidate.id === activity.petId);
          return (
            <View key={activity.id} style={styles.activity}>
              <View style={styles.activityHeader}>
                <Text style={styles.activityTitle}>{activity.type.replace('_', ' ')}</Text>
                <Badge label={pet?.name ?? 'Pet'} />
              </View>
              <Text style={styles.activityMeta}>{new Date(activity.startedAt).toLocaleString()} · {Math.round(activity.durationSeconds / 60)} min{activity.distanceMeters ? ` · ${(activity.distanceMeters / 1000).toFixed(2)} km` : ''}</Text>
              {activity.notes ? <Text style={styles.activityMeta}>{activity.notes}</Text> : null}
            </View>
          );
        }) : <EmptyState title="No activities recorded" description="Use the form to record a walk, run, hike, ride, swim, or play session." />}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream50 },
  content: { padding: 20, gap: 16 },
  title: { color: colors.cream900, fontSize: 30, fontWeight: '900' },
  petPicker: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  activity: { borderTopColor: colors.cream200, borderTopWidth: 1, paddingVertical: 12 },
  activityHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center', gap: 8 },
  activityTitle: { color: colors.cream900, fontSize: 16, fontWeight: '800', textTransform: 'capitalize' },
  activityMeta: { color: colors.cream600, fontSize: 13, marginTop: 4 },
});
