import { useEffect, useMemo, useState } from 'react';
import { Link } from 'react-router-dom';
import { checkIn, createLostPetAlert, createPlaydate, getLostPets, getNearbyPets, getPlaces, reportSighting, type LostPetAlert, type NearbyPet, type Place } from '../../api/petlife';
import { listPets } from '../../api/client';
import type { Pet } from '../../types';
import { Badge, BottomSheet, Button, Chip, EmptyState, ErrorMessage, Field, LoadingState, SelectInput, TextArea, TextInput } from '../../components/ui';
import { Avatar } from '../../components/ui';

type Filter = 'ALL' | 'PARK' | 'VET' | 'CAFE' | 'LOST';
const FILTERS: { key: Filter; label: string; emoji: string }[] = [
  { key: 'PARK', label: 'Parks', emoji: '🌳' },
  { key: 'VET', label: 'Vets', emoji: '🩺' },
  { key: 'CAFE', label: 'Cafés', emoji: '☕' },
  { key: 'LOST', label: 'Lost pets', emoji: '⚠️' },
];
const PIN_EMOJI: Record<string, string> = { PARK: '🌳', VET: '🩺', CAFE: '☕', HOTEL: '🏨', GROOMING: '🛁', PET_SHOP: '🦴', TRAINING: '🎾', OTHER: '📍' };

function usePosition() {
  const [pos, setPos] = useState<{ lat: number; lng: number } | null>(null);
  const [denied, setDenied] = useState(false);
  useEffect(() => {
    navigator.geolocation?.getCurrentPosition(
      (p) => setPos({ lat: p.coords.latitude, lng: p.coords.longitude }),
      () => setDenied(true),
      { timeout: 8000 },
    );
  }, []);
  return { pos, denied };
}

export function MapPage() {
  const { pos, denied } = usePosition();
  const [filter, setFilter] = useState<Filter>('ALL');
  const [query, setQuery] = useState('');
  const [places, setPlaces] = useState<Place[] | null>(null);
  const [lost, setLost] = useState<LostPetAlert[]>([]);
  const [pets, setPets] = useState<Pet[]>([]);
  const [selected, setSelected] = useState<Place | null>(null);
  const [alert, setAlert] = useState<LostPetAlert | null>(null);
  const [playdateOpen, setPlaydateOpen] = useState(false);
  const [lostOpen, setLostOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const center = pos ?? { lat: -23.56, lng: -46.64 };

  useEffect(() => { listPets().then(setPets).catch(() => setPets([])); }, []);
  useEffect(() => {
    if (!pos && !denied) return;
    getPlaces(center.lat, center.lng, 8000, filter === 'ALL' || filter === 'LOST' ? undefined : filter, query || undefined)
      .then((r) => setPlaces(r.items ?? [])).catch((e) => { setError(e instanceof Error ? e.message : 'Could not load places'); setPlaces([]); });
    getLostPets(center.lat, center.lng, 10000).then(setLost).catch(() => setLost([]));
  }, [pos, denied, filter, query, center.lat, center.lng]);

  const nearLost = useMemo(() => lost.filter((l) => l.status === 'ACTIVE'), [lost]);
  const pins = (filter === 'LOST' ? [] : places ?? []);

  if (places === null && !denied && !error) return <LoadingState label="Finding pet-friendly places…" />;

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <TextInput value={query} onChange={(e) => setQuery(e.target.value)} placeholder="Search nearby…" aria-label="Search nearby" className="min-w-48 flex-1" />
        {FILTERS.map((f) => <Chip key={f.key} selected={filter === f.key} onClick={() => setFilter(filter === f.key ? 'ALL' : f.key)}>{f.emoji} {f.label}</Chip>)}
        <Button variant="primary" size="sm" onClick={() => setPlaydateOpen(true)}>🐶 Playdate now</Button>
        <Button variant="outline" size="sm" onClick={() => setLostOpen(true)}>⚠️ Report lost pet</Button>
      </div>
      <ErrorMessage message={denied ? 'Location permission was denied — showing your default area. Enable location for nearby results.' : error} />

      <div className="grid gap-4 lg:grid-cols-[1fr_340px]">
        <div className="pl-card relative min-h-[420px] overflow-hidden">
          <div className="pl-map-grid absolute inset-0" aria-hidden="true" />
          <svg className="absolute inset-0 h-full w-full" aria-hidden="true" preserveAspectRatio="none" viewBox="0 0 100 100">
            <path d="M-5 60 C 20 40, 40 80, 60 55 S 90 30, 105 50" fill="none" stroke="var(--color-teal-light)" strokeWidth="6" />
            <path d="M30 -5 C 40 30, 20 70, 45 105" fill="none" stroke="var(--color-teal-light)" strokeWidth="5" />
          </svg>
          <div className="absolute left-1/2 top-1/2 -translate-x-1/2 -translate-y-1/2" aria-label="You are here">
            <span className="block h-5 w-5 rounded-full border-4 border-white bg-teal shadow-card" />
            <span className="pl-pulse absolute inset-0 -z-10 rounded-full bg-teal/40" />
          </div>
          {pins.slice(0, 40).map((pl, i) => (
            <button key={pl.id} type="button" onClick={() => setSelected(pl)} aria-label={pl.name}
              className="absolute grid h-10 w-10 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full bg-white text-lg shadow-card transition hover:scale-110"
              style={{ left: `${12 + ((i * 37) % 76)}%`, top: `${14 + ((i * 53) % 68)}%` }}>
              {PIN_EMOJI[pl.category ?? 'OTHER']}
            </button>
          ))}
          {nearLost.map((l, i) => (
            <button key={l.id} type="button" onClick={() => setAlert(l)} aria-label={`Lost pet alert: ${l.petName}`}
              className="absolute grid h-10 w-10 -translate-x-1/2 -translate-y-1/2 place-items-center rounded-full bg-danger-soft text-lg ring-2 ring-danger"
              style={{ left: `${20 + ((i * 29) % 60)}%`, top: `${18 + ((i * 41) % 55)}%` }}>
              ⚠️
            </button>
          ))}
          {nearLost[0] ? (
            <div className="absolute bottom-3 left-3 right-3 rounded-lg bg-white/95 p-3 shadow-card sm:right-auto sm:max-w-sm">
              <p className="text-label font-extrabold text-danger">Lost pet · {nearLost[0].distanceMeters ? `${(nearLost[0].distanceMeters / 1000).toFixed(1)} km away` : 'nearby'}</p>
              <p className="mt-1 font-extrabold text-ink">{nearLost[0].petName}</p>
              <p className="text-caption text-muted line-clamp-2">{nearLost[0].description}</p>
              <div className="mt-2 flex gap-2">
                <Button size="sm" variant="secondary" onClick={() => setAlert(nearLost[0])}>I have information</Button>
                <Button size="sm" variant="ghost" onClick={() => navigator.share?.({ title: `Lost pet: ${nearLost[0].petName}`, text: nearLost[0].description ?? '' }).catch(() => {})}>Share alert</Button>
              </div>
            </div>
          ) : null}
        </div>

        <div className="space-y-3">
          <h2 className="text-title">Near you</h2>
          <PlaceList places={filter === 'LOST' ? [] : pins} onSelect={setSelected} />
          {filter !== 'LOST' && pins.length === 0 ? <EmptyState title="No places found" description="Try another filter or widen the area." /> : null}
        </div>
      </div>

      <PlaceSheet place={selected} pets={pets} onClose={() => setSelected(null)} />
      <LostPetSheet alert={alert} onClose={() => setAlert(null)} />
      <PlaydateSheet open={playdateOpen} pets={pets} center={center} onClose={() => setPlaydateOpen(false)} />
      <ReportLostSheet open={lostOpen} pets={pets} center={center} onClose={() => setLostOpen(false)} />
    </div>
  );
}

function PlaceList({ places, onSelect }: { places: Place[]; onSelect: (p: Place) => void }) {
  return (
    <div className="space-y-2">
      {places.map((p) => (
        <button key={p.id} type="button" onClick={() => onSelect(p)} className="pl-card block w-full p-4 text-left">
          <div className="flex items-center gap-3">
            <span className="text-2xl" aria-hidden="true">{PIN_EMOJI[p.category ?? 'OTHER']}</span>
            <div className="min-w-0 flex-1">
              <p className="truncate font-extrabold text-ink">{p.name} {p.verifiedPartner ? <span className="text-teal">✓</span> : null}</p>
              <p className="text-caption text-muted">{p.category?.toLowerCase().replace('_', ' ')}{p.distanceMeters ? ` · ${(p.distanceMeters / 1000).toFixed(1)} km` : ''}{p.checkInsToday ? ` · ${p.checkInsToday} check-ins today` : ''}</p>
            </div>
            {p.rating ? <Badge tone="warning">★ {p.rating.toFixed(1)}</Badge> : null}
          </div>
        </button>
      ))}
    </div>
  );
}

function PlaceSheet({ place, pets, onClose }: { place: Place | null; pets: Pet[]; onClose: () => void }) {
  const [petId, setPetId] = useState('');
  const [done, setDone] = useState(false);
  const pet = petId || pets[0]?.id;
  if (!place) return null;
  return (
    <BottomSheet open title={place.name ?? 'Place'} onClose={onClose}>
      <div className="space-y-3">
        <div className="flex gap-2">
          <Badge tone="accent">{place.category?.toLowerCase().replace('_', ' ')}</Badge>
          {place.verifiedPartner ? <Badge tone="success">Verified partner</Badge> : null}
          {place.rating ? <Badge tone="warning">★ {place.rating.toFixed(1)} ({place.ratingCount})</Badge> : null}
        </div>
        {place.address ? <p className="text-body text-muted">📍 {place.address}</p> : null}
        {place.description ? <p className="text-body text-ink">{place.description}</p> : null}
        {pets.length > 1 ? (
          <SelectInput label="Check in as" value={pet} onChange={(e) => setPetId(e.target.value)}>
            {pets.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
          </SelectInput>
        ) : null}
        {done ? <p className="rounded-md bg-teal-light p-3 text-body font-bold text-teal">Checked in! Enjoy your visit. 🐾</p> : (
          <Button className="w-full" onClick={() => pet && place.id && checkIn(place.id, pet).then(() => setDone(true)).catch(() => {})} disabled={!pet}>
            Check in here
          </Button>
        )}
      </div>
    </BottomSheet>
  );
}

function LostPetSheet({ alert, onClose }: { alert: LostPetAlert | null; onClose: () => void }) {
  const [msg, setMsg] = useState('');
  const [sent, setSent] = useState(false);
  if (!alert) return null;
  return (
    <BottomSheet open title={`Lost pet: ${alert.petName}`} onClose={onClose}>
      <div className="space-y-3">
        <Badge tone="danger">{alert.status === 'ACTIVE' ? 'Active alert' : alert.status}</Badge>
        {alert.photoUrl ? <img src={alert.photoUrl} alt={alert.petName} className="h-44 w-full rounded-lg object-cover" /> : null}
        <p className="text-body text-ink">{alert.description}</p>
        <p className="text-label text-muted">{alert.petSpecies} · seen {alert.lastSeenAt ? new Date(alert.lastSeenAt).toLocaleString() : 'recently'} · {alert.sightings ?? 0} sightings</p>
        {sent ? <p className="rounded-md bg-teal-light p-3 font-bold text-teal">Thank you! The owner has been notified.</p> : (
          <form className="space-y-2" onSubmit={(e) => { e.preventDefault(); if (msg.trim() && alert.id) reportSighting(alert.id, { latitude: alert.latitude ?? null, longitude: alert.longitude ?? null, seenAt: new Date().toISOString(), note: msg }).then(() => setSent(true)).catch(() => {}); }}>
            <TextArea rows={2} value={msg} onChange={(e) => setMsg(e.target.value)} placeholder="Where did you see this pet?" label="I have information" />
            <Button className="w-full" disabled={!msg.trim()}>Send sighting</Button>
          </form>
        )}
      </div>
    </BottomSheet>
  );
}

function PlaydateSheet({ open, pets, center, onClose }: { open: boolean; pets: Pet[]; center: { lat: number; lng: number }; onClose: () => void }) {
  const [nearby, setNearby] = useState<NearbyPet[] | null>(null);
  const [petId, setPetId] = useState('');
  const [sent, setSent] = useState<string | null>(null);
  useEffect(() => {
    if (open) { setNearby(null); setSent(null); getNearbyPets(center.lat, center.lng).then(setNearby).catch(() => setNearby([])); }
  }, [open, center.lat, center.lng]);
  const mine = petId || pets[0]?.id;
  return (
    <BottomSheet open={open} title="Playdate now" onClose={onClose}>
      {pets.length > 1 ? (
        <SelectInput label="Your pet" value={mine} onChange={(e) => setPetId(e.target.value)}>
          {pets.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
        </SelectInput>
      ) : null}
      <div className="mt-3 space-y-2">
        {nearby === null ? <LoadingState label="Finding nearby pets…" /> : nearby.length === 0 ? (
          <p className="rounded-lg bg-sand p-4 text-body text-muted">No pets nearby right now. Try again later or check the map for busy parks.</p>
        ) : nearby.map((p) => (
          <div key={p.petId} className="flex items-center gap-3 rounded-lg bg-sand p-3">
            <Avatar name={p.petName ?? '?'} imageUrl={p.avatarUrl} size="sm" />
            <div className="min-w-0 flex-1">
              <Link to={`/app/pets/${p.petId}`} className="font-extrabold text-ink hover:underline">{p.petName}</Link>
              <p className="text-caption text-muted">{p.species}{p.breed ? ` · ${p.breed}` : ''} · {(p.distanceMeters ?? 0) / 1000 < 1 ? `${Math.round(p.distanceMeters ?? 0)}m` : `${((p.distanceMeters ?? 0) / 1000).toFixed(1)}km`} away</p>
            </div>
            {sent === p.petId ? <Badge tone="success">Invited!</Badge> : (
              <Button size="sm" onClick={() => mine && p.petId && createPlaydate({ petId: mine, mode: 'NOW', inviteePetIds: [p.petId], latitude: center.lat, longitude: center.lng }).then(() => setSent(p.petId!)).catch(() => {})}>Invite</Button>
            )}
          </div>
        ))}
      </div>
    </BottomSheet>
  );
}

function ReportLostSheet({ open, pets, center, onClose }: { open: boolean; pets: Pet[]; center: { lat: number; lng: number }; onClose: () => void }) {
  const [petId, setPetId] = useState('');
  const [description, setDescription] = useState('');
  const [phone, setPhone] = useState('');
  const [radius, setRadius] = useState('2000');
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const pet = petId || pets[0]?.id;
  return (
    <BottomSheet open={open} title="Report lost pet" onClose={onClose}>
      {done ? <p className="rounded-md bg-teal-light p-4 font-bold text-teal">Alert published. Nearby PetLife members will be notified. Stay close — sightings will appear here.</p> : (
        <form className="space-y-3" onSubmit={(e) => {
          e.preventDefault();
          if (!pet || !description.trim()) return;
          createLostPetAlert({ petId: pet, description, contactPhone: phone || undefined, latitude: center.lat, longitude: center.lng, lastSeenAt: new Date().toISOString(), radiusMeters: Number(radius) || 2000 })
            .then(() => setDone(true)).catch((err) => setError(err instanceof Error ? err.message : 'Could not publish the alert'));
        }}>
          {pets.length > 1 ? (
            <SelectInput label="Pet" required value={pet} onChange={(e) => setPetId(e.target.value)}>
              {pets.map((p) => <option key={p.id} value={p.id}>{p.name}</option>)}
            </SelectInput>
          ) : null}
          <TextArea label="Description" required rows={3} value={description} onChange={(e) => setDescription(e.target.value)} placeholder="Collar color, temperament, where it was last seen…" />
          <TextInput label="Contact phone" value={phone} onChange={(e) => setPhone(e.target.value)} inputMode="tel" />
          <Field label="Alert radius" hint="People within this distance get notified">
            <SelectInput value={radius} onChange={(e) => setRadius(e.target.value)}>
              <option value="1000">1 km</option><option value="2000">2 km</option><option value="5000">5 km</option><option value="10000">10 km</option>
            </SelectInput>
          </Field>
          <ErrorMessage message={error} />
          <Button className="w-full">Publish alert</Button>
        </form>
      )}
    </BottomSheet>
  );
}
