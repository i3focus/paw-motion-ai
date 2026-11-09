import { useEffect, useState } from 'react';
import { ApiError, createPet, listPets, updatePet } from '../api/client';
import { PetForm, type PetFormInput } from '../components/PetForm';
import { PetCard } from '../components/petVisuals';
import { Button, Card, EmptyState, ErrorMessage, SectionHeader } from '../components/ui';
import type { Pet } from '../types';

export function PetsPage() {
  const [pets, setPets] = useState<Pet[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [editingPet, setEditingPet] = useState<Pet | null>(null);
  const [formOpen, setFormOpen] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  async function refresh() {
    setPets(await listPets());
  }

  useEffect(() => {
    refresh().catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load pets'));
  }, []);

  async function handleSubmit(input: PetFormInput) {
    setError(null);
    setSubmitting(true);
    try {
      if (editingPet) {
        await updatePet(editingPet.id, input);
      } else {
        await createPet(input);
      }
      setEditingPet(null);
      setFormOpen(false);
      await refresh();
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : editingPet ? 'Unable to update pet' : 'Unable to create pet');
    } finally {
      setSubmitting(false);
    }
  }

  function startCreate() {
    setEditingPet(null);
    setFormOpen(true);
    setError(null);
  }

  function startEdit(pet: Pet) {
    setEditingPet(pet);
    setFormOpen(true);
    setError(null);
  }

  function cancelForm() {
    setEditingPet(null);
    setFormOpen(false);
    setError(null);
  }

  return (
    <div className="space-y-6">
      <SectionHeader
        title="Pets"
        subtitle="Create pet profiles and keep their details up to date."
        action={!formOpen ? <Button onClick={startCreate}>Add pet</Button> : undefined}
      />
      <ErrorMessage message={error && !formOpen ? error : null} />
      <div className="grid gap-6 lg:grid-cols-[420px_1fr]">
        {formOpen ? (
          <Card title={editingPet ? `Edit ${editingPet.name}` : 'Create pet profile'} description="Keep the profile accurate so activity insights stay relevant.">
            <PetForm
              key={editingPet?.id ?? 'new-pet'}
              initialPet={editingPet}
              submitLabel={editingPet ? 'Save changes' : 'Add pet'}
              submitting={submitting}
              error={error}
              onSubmit={handleSubmit}
              onCancel={cancelForm}
            />
          </Card>
        ) : null}

        <Card title="Your pets" description="Registered pets appear here with their profile details.">
          {pets.length === 0 ? (
            <EmptyState
              title="No pets yet"
              description="Add your first pet profile to start recording activities."
              action={<Button onClick={startCreate}>Add pet</Button>}
            />
          ) : (
            <div className="grid gap-4 sm:grid-cols-2">
              {pets.map((pet) => (
                <PetCard
                  key={pet.id}
                  pet={pet}
                  action={<Button variant="secondary" onClick={() => startEdit(pet)}>Edit</Button>}
                />
              ))}
            </div>
          )}
        </Card>
      </div>
    </div>
  );
}
