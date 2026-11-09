import { useCallback, useEffect, useState } from 'react';
import { ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, createPet, listPets, updatePet } from '../api/client';
import { Button, Card, EmptyState, ErrorText, Field, Input } from '../components';
import { PetCard } from '../petVisuals';
import type { Pet } from '../types';
import { colors } from '../theme';
import { isNotFutureDate, isValidDateInput } from '../validation';

const speciesOptions = ['dog', 'cat', 'bird', 'rabbit', 'other'];
const sizeOptions = ['small', 'medium', 'large'];

export function PetsScreen() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editingPet, setEditingPet] = useState<Pet | null>(null);
  const [name, setName] = useState('');
  const [species, setSpecies] = useState('dog');
  const [size, setSize] = useState('medium');
  const [breed, setBreed] = useState('');
  const [birthDate, setBirthDate] = useState('');
  const [preference, setPreference] = useState('');
  const [loading, setLoading] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

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

  function resetForm() {
    setEditingPet(null);
    setName('');
    setSpecies('dog');
    setSize('medium');
    setBreed('');
    setBirthDate('');
    setPreference('');
    setFieldErrors({});
  }

  function startEdit(pet: Pet) {
    setEditingPet(pet);
    setName(pet.name);
    setSpecies(pet.species);
    setSize(pet.size);
    setBreed(pet.breed ?? '');
    setBirthDate(pet.birthDate ?? '');
    setPreference(pet.activityPreference ?? '');
    setFieldErrors({});
    setError(null);
  }

  async function submit() {
    const errors: Record<string, string> = {};
    const trimmedName = name.trim();
    const trimmedBreed = breed.trim();
    const trimmedPreference = preference.trim();
    if (!trimmedName) {
      errors.name = 'Pet name is required';
    } else if (trimmedName.length > 80) {
      errors.name = 'Name must be 80 characters or fewer';
    }
    if (!speciesOptions.includes(species)) {
      errors.species = `Species must be one of: ${speciesOptions.join(', ')}`;
    }
    if (!sizeOptions.includes(size)) {
      errors.size = `Size must be one of: ${sizeOptions.join(', ')}`;
    }
    if (trimmedBreed.length > 80) {
      errors.breed = 'Breed must be 80 characters or fewer';
    }
    if (birthDate && (!isValidDateInput(birthDate) || !isNotFutureDate(birthDate))) {
      errors.birthDate = 'Use YYYY-MM-DD and a date that is not in the future';
    }
    if (trimmedPreference.length > 120) {
      errors.activityPreference = 'Activity preference must be 120 characters or fewer';
    }
    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    setError(null);
    setLoading(true);
    try {
      const input = {
        name: trimmedName,
        species,
        size,
        breed: trimmedBreed || undefined,
        birthDate: birthDate || undefined,
        activityPreference: trimmedPreference || undefined,
      };
      if (editingPet) {
        await updatePet(editingPet.id, input);
      } else {
        await createPet(input);
      }
      resetForm();
      await load();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : editingPet ? 'Unable to update pet' : 'Unable to create pet');
    } finally {
      setLoading(false);
    }
  }

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content}>
      <Text style={styles.title}>Pets</Text>
      <Card title={editingPet ? `Edit ${editingPet.name}` : 'Create pet profile'}>
        <Field label="Name"><Input value={name} onChangeText={setName} maxLength={80} /></Field>
        <ErrorText message={fieldErrors.name ?? null} />
        <Field label="Species"><Input value={species} onChangeText={setSpecies} placeholder={speciesOptions.join(', ')} autoCapitalize="none" /></Field>
        <ErrorText message={fieldErrors.species ?? null} />
        <Field label="Size"><Input value={size} onChangeText={setSize} placeholder={sizeOptions.join(', ')} autoCapitalize="none" /></Field>
        <ErrorText message={fieldErrors.size ?? null} />
        <Field label="Breed"><Input value={breed} onChangeText={setBreed} maxLength={80} /></Field>
        <ErrorText message={fieldErrors.breed ?? null} />
        <Field label="Birth date (YYYY-MM-DD)"><Input value={birthDate} onChangeText={setBirthDate} placeholder="2020-01-01" autoCapitalize="none" /></Field>
        <ErrorText message={fieldErrors.birthDate ?? null} />
        <Field label="Activity preference"><Input value={preference} onChangeText={setPreference} maxLength={120} placeholder="Daily walks" /></Field>
        <ErrorText message={fieldErrors.activityPreference ?? null} />
        <ErrorText message={error} />
        <View style={styles.formButtons}>
          <Button label={loading ? 'Saving…' : editingPet ? 'Save changes' : 'Add pet'} onPress={submit} disabled={loading} />
          {editingPet ? <Button label="Cancel" variant="secondary" onPress={resetForm} disabled={loading} /> : null}
        </View>
      </Card>
      <Card title="Your pets">
        {pets.length ? pets.map((pet) => (
          <PetCard key={pet.id} pet={pet} action={<Button label="Edit" variant="secondary" onPress={() => startEdit(pet)} />} />
        )) : <EmptyState title="No pets yet" description="Add your first pet profile to start recording activities." />}
      </Card>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream50 },
  content: { padding: 20, gap: 16 },
  title: { color: colors.cream900, fontSize: 30, fontWeight: '900' },
  formButtons: { flexDirection: 'row', gap: 10 },
});
