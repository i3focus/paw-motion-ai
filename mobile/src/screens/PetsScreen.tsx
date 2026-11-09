import { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, createPet, listPets } from '../api/client';
import { Badge, Button, Card, EmptyState, ErrorText, Field, Input } from '../components';
import type { Pet } from '../types';
import { colors } from '../theme';

const speciesOptions = ['dog', 'cat', 'bird', 'rabbit', 'other'];
const sizeOptions = ['small', 'medium', 'large'];

export function PetsScreen() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState('');
  const [species, setSpecies] = useState('dog');
  const [size, setSize] = useState('medium');
  const [breed, setBreed] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [preference, setPreference] = useState('');
  const [loading, setLoading] = useState(false);

  const load = useCallback(async () => {
    try {
      setPets(await listPets());
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load pets');
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  async function submit() {
    setError(null);
    setLoading(true);
    try {
      await createPet({ name, species, size, breed: breed || undefined, birthDate: birthDate || undefined, activityPreference: preference || undefined });
      setName('');
      setBreed('');
      setBirthDate('');
      setPreference('');
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to create pet');
    } finally {
      setLoading(false);
    }
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Pets</Text>
      <Card title="Create pet profile">
        <Field label="Name"><Input value={name} onChangeText={setName} /></Field>
        <Field label="Species"><Input value={species} onChangeText={setSpecies} placeholder={speciesOptions.join(', ')} /></Field>
        <Field label="Size"><Input value={size} onChangeText={setSize} placeholder={sizeOptions.join(', ')} /></Field>
        <Field label="Breed"><Input value={breed} onChangeText={setBreed} /></Field>
        <Field label="Birth date (YYYY-MM-DD)"><Input value={birthDate} onChangeText={setBirthDate} placeholder="2020-01-01" /></Field>
        <Field label="Activity preference"><Input value={preference} onChangeText={setPreference} placeholder="Daily walks" /></Field>
        <ErrorText message={error} />
        <Button label={loading ? 'Saving…' : 'Add pet'} onPress={submit} disabled={loading || !name.trim()} />
      </Card>
      <Card title="Your pets">
        {pets.length ? pets.map((pet) => (
          <View key={pet.id} style={styles.pet}>
            <View style={styles.petHeader}>
              <Text style={styles.petName}>{pet.name}</Text>
              <Badge label={pet.status} tone="accent" />
            </View>
            <Text style={styles.petMeta}>{pet.species} · {pet.size}{pet.breed ? ` · ${pet.breed}` : ''}</Text>
            {pet.activityPreference ? <Text style={styles.petMeta}>Preference: {pet.activityPreference}</Text> : null}
          </View>
        )) : <EmptyState title="No pets yet" description="Add your first pet profile to start recording activities." />}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream50 },
  content: { padding: 20, gap: 16 },
  title: { color: colors.cream900, fontSize: 30, fontWeight: '900' },
  pet: { borderTopColor: colors.cream200, borderTopWidth: 1, paddingVertical: 12 },
  petHeader: { flexDirection: 'row', justifyContent: 'space-between', alignItems: 'center' },
  petName: { color: colors.cream900, fontSize: 17, fontWeight: '800', textTransform: 'capitalize' },
  petMeta: { color: colors.cream600, fontSize: 14, marginTop: 4 },
});
