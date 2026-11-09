import { useState, type FormEvent } from 'react';
import { isNotFutureDate, isValidDateInput } from '../lib/validation';
import { Button, DateInput, ErrorMessage, SelectInput, TextInput } from './ui';
import type { Pet } from '../types';

export type PetFormInput = {
  name: string;
  species: string;
  breed?: string;
  birthDate?: string;
  size: string;
  activityPreference?: string;
};

const speciesOptions = ['dog', 'cat', 'bird', 'rabbit', 'other'];
const sizeOptions = ['small', 'medium', 'large'];

type PetFormProps = {
  initialPet?: Pet | null;
  submitLabel: string;
  submitting: boolean;
  error?: string | null;
  onSubmit: (input: PetFormInput) => Promise<void> | void;
  onCancel?: () => void;
};

export function PetForm({ initialPet, submitLabel, submitting, error, onSubmit, onCancel }: PetFormProps) {
  const [name, setName] = useState(initialPet?.name ?? '');
  const [species, setSpecies] = useState(initialPet?.species ?? 'dog');
  const [size, setSize] = useState(initialPet?.size ?? 'medium');
  const [breed, setBreed] = useState(initialPet?.breed ?? '');
  const [birthDate, setBirthDate] = useState(initialPet?.birthDate ?? '');
  const [activityPreference, setActivityPreference] = useState(initialPet?.activityPreference ?? '');
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const errors: Record<string, string> = {};
    const trimmedName = name.trim();
    const trimmedBreed = breed.trim();
    const trimmedPreference = activityPreference.trim();

    if (!trimmedName) {
      errors.name = 'Pet name is required';
    } else if (trimmedName.length > 80) {
      errors.name = 'Name must be 80 characters or fewer';
    }
    if (trimmedBreed.length > 80) {
      errors.breed = 'Breed must be 80 characters or fewer';
    }
    if (birthDate && (!isValidDateInput(birthDate) || !isNotFutureDate(birthDate))) {
      errors.birthDate = 'Birth date must be a valid date that is not in the future';
    }
    if (trimmedPreference.length > 120) {
      errors.activityPreference = 'Activity preference must be 120 characters or fewer';
    }

    setFieldErrors(errors);
    if (Object.keys(errors).length > 0) {
      return;
    }

    await onSubmit({
      name: trimmedName,
      species,
      size,
      breed: trimmedBreed || undefined,
      birthDate: birthDate || undefined,
      activityPreference: trimmedPreference || undefined,
    });
  }

  return (
    <form onSubmit={handleSubmit} className="space-y-4" noValidate>
      <TextInput
        label="Name"
        required
        value={name}
        onChange={(event) => setName(event.target.value)}
        error={fieldErrors.name}
        maxLength={80}
        placeholder="Luna"
      />
      <div className="grid grid-cols-2 gap-3">
        <SelectInput label="Species" value={species} onChange={(event) => setSpecies(event.target.value)}>
          {speciesOptions.map((option) => <option key={option} value={option}>{option}</option>)}
        </SelectInput>
        <SelectInput label="Size" value={size} onChange={(event) => setSize(event.target.value)}>
          {sizeOptions.map((option) => <option key={option} value={option}>{option}</option>)}
        </SelectInput>
      </div>
      <TextInput
        label="Breed"
        value={breed}
        onChange={(event) => setBreed(event.target.value)}
        error={fieldErrors.breed}
        maxLength={80}
        placeholder="Golden Retriever"
      />
      <DateInput
        label="Birth date"
        value={birthDate}
        onChange={(event) => setBirthDate(event.target.value)}
        error={fieldErrors.birthDate}
        max={new Date().toISOString().slice(0, 10)}
      />
      <TextInput
        label="Activity preference"
        value={activityPreference}
        onChange={(event) => setActivityPreference(event.target.value)}
        error={fieldErrors.activityPreference}
        maxLength={120}
        placeholder="Daily walks"
      />
      <ErrorMessage message={error} />
      <div className="flex flex-col-reverse gap-2 sm:flex-row">
        {onCancel ? <Button type="button" variant="secondary" onClick={onCancel} className="sm:flex-1">Cancel</Button> : null}
        <Button type="submit" disabled={submitting} className="sm:flex-1">{submitting ? 'Saving…' : submitLabel}</Button>
      </div>
    </form>
  );
}
