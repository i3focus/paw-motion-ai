import { useEffect, useState } from 'react';
import { listPets } from '../api/client';
import { createBooking } from '../api/petlife';
import type { Pet } from '../types';
import { BottomSheet, Button, Chip, ErrorMessage, TextArea, TextInput } from './ui';

export function BookingSheet({
  open,
  onClose,
  serviceId,
  partnerName,
  campaignId,
}: {
  open: boolean;
  onClose: () => void;
  serviceId: string;
  partnerName?: string | null;
  campaignId?: string | null;
}) {
  const [pets, setPets] = useState<Pet[]>([]);
  const [petId, setPetId] = useState('');
  const [startsAt, setStartsAt] = useState('');
  const [notes, setNotes] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [done, setDone] = useState(false);

  useEffect(() => {
    if (open) {
      listPets().then(setPets).catch(() => setPets([]));
      setPetId('');
      setStartsAt('');
      setNotes('');
      setDone(false);
      setError(null);
      setBusy(false);
    }
  }, [open]);

  const resolvedPet = petId || pets[0]?.id || '';

  async function book() {
    if (!resolvedPet || !startsAt) { setError('Pick a pet and a time'); return; }
    const start = new Date(startsAt);
    if (Number.isNaN(start.getTime()) || start.getTime() <= Date.now() + 30 * 60_000) {
      setError('Bookings need at least a 30-minute lead time');
      return;
    }
    setBusy(true);
    setError(null);
    try {
      await createBooking({ serviceId, petId: resolvedPet, startsAt: start.toISOString(), notes: notes.trim() || undefined, campaignId: campaignId ?? undefined });
      setDone(true);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not create the booking');
    } finally {
      setBusy(false);
    }
  }

  return (
    <BottomSheet open={open} onClose={onClose} title={partnerName ? `Book at ${partnerName}` : 'Book a service'}>
      {done ? (
        <div className="py-6 text-center">
          <p className="text-4xl">📅</p>
          <p className="mt-3 text-title">Request sent!</p>
          <p className="mt-1 text-body text-muted">{partnerName ?? 'The partner'} will confirm your booking shortly.</p>
          <Button className="mt-4" onClick={onClose}>Done</Button>
        </div>
      ) : (
        <div className="space-y-4">
          {pets.length > 1 ? (
            <div>
              <p className="mb-2 text-label font-bold text-muted">Book for</p>
              <div className="flex flex-wrap gap-2">
                {pets.map((p) => <Chip key={p.id} selected={resolvedPet === p.id} onClick={() => setPetId(p.id)}>{p.name}</Chip>)}
              </div>
            </div>
          ) : null}
          <TextInput label="Date & time" required type="datetime-local" value={startsAt} onChange={(e) => setStartsAt(e.target.value)}
            min={new Date(Date.now() + 30 * 60_000 - new Date().getTimezoneOffset() * 60_000).toISOString().slice(0, 16)} hint="At least 30 minutes from now" />
          <TextArea label="Notes" rows={2} maxLength={500} value={notes} onChange={(e) => setNotes(e.target.value)} placeholder="Anything the partner should know…" />
          <ErrorMessage message={error} />
          <Button onClick={book} disabled={busy || !startsAt || pets.length === 0} className="w-full">
            {busy ? 'Booking…' : 'Request booking'}
          </Button>
          {pets.length === 0 ? <p className="text-center text-label text-muted">Add a pet first to book a service.</p> : null}
        </div>
      )}
    </BottomSheet>
  );
}
