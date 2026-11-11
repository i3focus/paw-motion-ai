import { useEffect, useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../auth';
import { createPet, listPets, updateProfile } from '../../api/client';
import { cancelBooking, getNotificationPreferences, listMyBookings, updateNotificationPreferences, type Booking, type NotificationPreferences } from '../../api/petlife';
import { PetForm, type PetFormInput } from '../../components/PetForm';
import type { Pet } from '../../types';
import { Avatar, BottomSheet, Button, ErrorMessage, LoadingState, SectionHeader } from '../../components/ui';
import { PetAvatar } from '../../components/petVisuals';
import { TextInput, TextArea } from '../../components/ui';

export function ProfilePage() {
  const { owner, logout, refreshProfile } = useAuth();
  const navigate = useNavigate();
  const [pets, setPets] = useState<Pet[] | null>(null);
  const [addOpen, setAddOpen] = useState(false);
  const [editOpen, setEditOpen] = useState(false);
  const [prefs, setPrefs] = useState<NotificationPreferences | null>(null);
  const [bookings, setBookings] = useState<Booking[] | null>(null);
  const [savingPet, setSavingPet] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [name, setName] = useState(owner?.displayName ?? '');
  const [bio, setBio] = useState(owner?.bio ?? '');

  useEffect(() => { listPets().then(setPets).catch(() => setPets([])); }, [addOpen]);
  useEffect(() => { getNotificationPreferences().then(setPrefs).catch(() => setPrefs({})); }, []);
  const [hasMoreBookings, setHasMoreBookings] = useState(false);
  const [loadingMoreBookings, setLoadingMoreBookings] = useState(false);

  useEffect(() => {
    listMyBookings().then((r) => { setBookings(r.items ?? []); setHasMoreBookings(Boolean(r.hasNext)); }).catch(() => setBookings([]));
  }, []);

  async function loadMoreBookings() {
    const page = Math.ceil((bookings?.length ?? 0) / 30);
    setLoadingMoreBookings(true);
    try {
      const r = await listMyBookings(page);
      setBookings((b) => [...(b ?? []), ...(r.items ?? [])]);
      setHasMoreBookings(Boolean(r.hasNext));
    } catch { /* keep loaded items */ } finally {
      setLoadingMoreBookings(false);
    }
  }

  async function addPet(input: PetFormInput) {
    setSavingPet(true);
    setError(null);
    try {
      await createPet(input);
      setAddOpen(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : 'Could not create pet');
    } finally {
      setSavingPet(false);
    }
  }

  const prefRows: { key: keyof NotificationPreferences; label: string; hint: string }[] = [
    { key: 'healthReminders', label: 'Health reminders', hint: 'Vaccines and vet visits coming due' },
    { key: 'lostPets', label: 'Lost pets nearby', hint: 'Alerts within your area' },
    { key: 'playdates', label: 'Playdates', hint: 'Invites and responses' },
    { key: 'social', label: 'Social', hint: 'Reactions, comments, follows, badges and challenges' },
    { key: 'bookings', label: 'Bookings', hint: 'Reservation updates from partners' },
  ];

  return (
    <div className="mx-auto max-w-2xl space-y-6">
      <section className="pl-card p-5">
        <div className="flex items-center gap-4">
          <Avatar name={owner?.displayName ?? '?'} imageUrl={owner?.avatarUrl} size="lg" />
          <div className="min-w-0 flex-1">
            <h1 className="text-heading">{owner?.displayName}</h1>
            <p className="text-body text-muted">{owner?.email}</p>
          </div>
          <Button variant="outline" size="sm" onClick={() => setEditOpen(true)}>Edit profile</Button>
        </div>
      </section>

      <section>
        <SectionHeader title="My pets" action={<Button size="sm" onClick={() => setAddOpen(true)}>+ Add pet</Button>} />
        <div className="mt-3 grid gap-3 sm:grid-cols-2">
          {pets === null ? <LoadingState /> : pets.map((p) => (
            <Link key={p.id} to={`/app/pets/${p.id}`} className="pl-card flex items-center gap-4 p-4">
              <PetAvatar pet={p} className="h-14 w-14" />
              <div className="min-w-0">
                <p className="truncate font-extrabold capitalize text-ink">{p.name}</p>
                <p className="text-caption capitalize text-muted">{p.species}{p.breed ? ` · ${p.breed}` : ''}</p>
              </div>
            </Link>
          ))}
          {pets !== null && pets.length === 0 ? (
            <button type="button" onClick={() => setAddOpen(true)} className="pl-card-sand flex flex-col items-center justify-center gap-2 p-8 text-label font-extrabold text-teal">
              <span className="text-3xl" aria-hidden="true">🐾</span>Add your first pet
            </button>
          ) : null}
        </div>
      </section>

      {bookings !== null && bookings.length > 0 ? (
        <section className="pl-card p-5">
          <h2 className="text-title">My bookings</h2>
          <div className="mt-3 divide-y divide-line">
            {bookings.map((b) => (
              <div key={b.id} className="flex items-center justify-between gap-3 py-3">
                <div className="min-w-0">
                  <p className="font-extrabold text-ink">{b.serviceName} <span className="font-semibold text-muted">· {b.partnerName}</span></p>
                  <p className="text-caption text-muted">{b.petName} · {new Date(b.startsAt ?? '').toLocaleString()} · {b.status?.toLowerCase()}</p>
                </div>
                {b.status === 'REQUESTED' || b.status === 'CONFIRMED' ? (
                  <Button variant="ghost" size="sm" onClick={() => b.id && cancelBooking(b.id).then(() => setBookings((l) => l?.map((x) => (x.id === b.id ? { ...x, status: 'CANCELLED' } : x)) ?? l)).catch(() => {})}>Cancel</Button>
                ) : null}
              </div>
            ))}
          </div>
          {hasMoreBookings ? (
            <Button variant="secondary" size="sm" onClick={loadMoreBookings} disabled={loadingMoreBookings} className="mt-3 w-full">
              {loadingMoreBookings ? 'Loading…' : 'Load more'}
            </Button>
          ) : null}
        </section>
      ) : null}

      <section className="pl-card p-5">
        <h2 className="text-title">Notifications</h2>
        <div className="mt-3 divide-y divide-line">
          {prefRows.map((row) => (
            <label key={row.key} className="flex items-center justify-between gap-4 py-3">
              <div><p className="font-extrabold text-ink">{row.label}</p><p className="text-caption text-muted">{row.hint}</p></div>
              <input type="checkbox" className="h-5 w-5 accent-[var(--color-teal)]" checked={Boolean(prefs?.[row.key] ?? true)}
                onChange={(e) => {
                  const next = { ...(prefs ?? {}), [row.key]: e.target.checked };
                  setPrefs(next);
                  updateNotificationPreferences(next).catch(() => {});
                }} />
            </label>
          ))}
        </div>
      </section>

      <section className="pl-card p-5">
        <h2 className="text-title">Account</h2>
        <div className="mt-3 flex flex-wrap gap-2">
          <Button variant="outline" size="sm" onClick={() => { logout(); navigate('/'); }}>Sign out</Button>
          <Link to="/partner"><Button variant="ghost" size="sm">I own a business — partner portal</Button></Link>
        </div>
      </section>

      <BottomSheet open={addOpen} title="Add a pet" onClose={() => setAddOpen(false)}>
        <ErrorMessage message={error} />
        <PetForm submitLabel={savingPet ? 'Saving…' : 'Add pet'} submitting={savingPet} error={error} onSubmit={addPet} onCancel={() => setAddOpen(false)} />
      </BottomSheet>

      <BottomSheet open={editOpen} title="Edit profile" onClose={() => setEditOpen(false)}>
        <form className="space-y-3" onSubmit={(e) => {
          e.preventDefault();
          updateProfile({ displayName: name.trim() || undefined, bio: bio.trim() || undefined })
            .then(() => { void refreshProfile(); setEditOpen(false); })
            .catch((err) => setError(err instanceof Error ? err.message : 'Could not save'));
        }}>
          <TextInput label="Display name" value={name} onChange={(e) => setName(e.target.value)} required />
          <TextArea label="Bio" rows={3} value={bio} onChange={(e) => setBio(e.target.value)} />
          <ErrorMessage message={error} />
          <Button className="w-full">Save</Button>
        </form>
      </BottomSheet>
    </div>
  );
}
