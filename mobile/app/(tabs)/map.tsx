import { useEffect, useState } from 'react';
import { FlatList, Pressable, StyleSheet, Text, View } from 'react-native';
import * as Location from 'expo-location';
import { useRouter } from 'expo-router';
import { checkIn, createLostPetAlert, createPlaydate, getLostPets, getNearbyPets, getPlaces, reportSighting, type LostPetAlert, type NearbyPet, type Place } from '../../src/api/petlife';
import { listPets } from '../../src/api/client';
import type { Pet } from '../../src/types';
import { Avatar, Sheet, timeAgo } from '../../src/petlife-ui';
import { Badge, Button, Chip, EmptyState, ErrorText, Field, Input, LoadingState } from '../../src/components';
import { colors, fonts, fontSize, radius, shadow } from '../../src/theme';

type Filter = 'ALL' | 'PARK' | 'VET' | 'CAFE' | 'LOST';
const FILTERS: { key: Filter; label: string }[] = [
  { key: 'PARK', label: '🌳 Parks' }, { key: 'VET', label: '🩺 Vets' }, { key: 'CAFE', label: '☕ Cafés' }, { key: 'LOST', label: '⚠️ Lost pets' },
];
const PIN: Record<string, string> = { PARK: '🌳', VET: '🩺', CAFE: '☕', HOTEL: '🏨', GROOMING: '🛁', PET_SHOP: '🦴', TRAINING: '🎾', OTHER: '📍' };
const FALLBACK = { lat: -23.56, lng: -46.64 };

export default function MapScreen() {
  const router = useRouter();
  const [center, setCenter] = useState(FALLBACK);
  const [denied, setDenied] = useState(false);
  const [filter, setFilter] = useState<Filter>('ALL');
  const [query, setQuery] = useState('');
  const [debouncedQuery, setDebouncedQuery] = useState('');
  const [places, setPlaces] = useState<Place[] | null>(null);
  const [lost, setLost] = useState<LostPetAlert[]>([]);
  const [pets, setPets] = useState<Pet[]>([]);
  const [selected, setSelected] = useState<Place | null>(null);
  const [alert, setAlert] = useState<LostPetAlert | null>(null);
  const [mode, setMode] = useState<'none' | 'playdate' | 'lost'>('none');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Location.requestForegroundPermissionsAsync()
      .then((p) => {
        if (!p.granted) { setDenied(true); return null; }
        return Location.getCurrentPositionAsync({ accuracy: Location.Accuracy.Balanced });
      })
      .then((pos) => { if (pos) setCenter({ lat: pos.coords.latitude, lng: pos.coords.longitude }); })
      .catch(() => setDenied(true));
    listPets().then(setPets).catch(() => setPets([]));
  }, []);

  useEffect(() => {
    const t = setTimeout(() => setDebouncedQuery(query.trim()), 350);
    return () => clearTimeout(t);
  }, [query]);

  useEffect(() => {
    let stale = false;
    getPlaces(center.lat, center.lng, 8000, filter === 'ALL' || filter === 'LOST' ? undefined : filter, debouncedQuery || undefined)
      .then((r) => { if (!stale) setPlaces(r.items ?? []); })
      .catch((e) => { if (!stale) { setError(e instanceof Error ? e.message : 'Could not load places'); setPlaces([]); } });
    getLostPets(center.lat, center.lng, 10000).then((l) => { if (!stale) setLost(l); }).catch(() => { if (!stale) setLost([]); });
    return () => { stale = true; };
  }, [center, filter, debouncedQuery]);

  const activeLost = lost.filter((l) => l.status === 'ACTIVE');
  const pins = filter === 'LOST' ? [] : places ?? [];

  return (
    <View style={s.screen}>
      <View style={s.controls}>
        <Input value={query} onChangeText={setQuery} placeholder="Search nearby…" style={s.search} />
        <FlatList horizontal data={FILTERS} keyExtractor={(f) => f.key} showsHorizontalScrollIndicator={false} contentContainerStyle={s.chips}
          renderItem={({ item }) => <Chip label={item.label} selected={filter === item.key} onPress={() => setFilter(filter === item.key ? 'ALL' : item.key)} />} />
        <View style={s.actions}>
          <View style={s.flex1}><Button label="🐶 Playdate now" onPress={() => setMode('playdate')} /></View>
          <View style={s.flex1}><Button label="⚠️ Lost pet" variant="outline" onPress={() => setMode('lost')} /></View>
        </View>
      </View>
      <ErrorText message={denied ? 'Location denied — showing a default area.' : error} />

      <View style={s.map}>
        <View style={s.me}><View style={s.meDot} /></View>
        {pins.slice(0, 30).map((pl, i) => (
          <Pressable key={pl.id} onPress={() => setSelected(pl)} accessibilityLabel={pl.name}
            style={[s.pin, { left: `${8 + ((i * 41) % 80)}%`, top: `${10 + ((i * 57) % 72)}%` }]}>
            <Text style={s.pinEmoji}>{PIN[pl.category ?? 'OTHER']}</Text>
          </Pressable>
        ))}
        {activeLost.map((l, i) => (
          <Pressable key={l.id} onPress={() => setAlert(l)} accessibilityLabel={`Lost pet alert: ${l.petName}`}
            style={[s.pin, s.pinLost, { left: `${16 + ((i * 31) % 66)}%`, top: `${14 + ((i * 43) % 60)}%` }]}>
            <Text style={s.pinEmoji}>⚠️</Text>
          </Pressable>
        ))}
        {activeLost[0] ? (
          <Pressable style={s.lostBanner} onPress={() => setAlert(activeLost[0] ?? null)}>
            <Text style={s.lostTitle} numberOfLines={1}>Lost pet · {activeLost[0].distanceMeters ? `${(activeLost[0].distanceMeters / 1000).toFixed(1)} km` : 'nearby'}</Text>
            <Text style={s.lostBody} numberOfLines={1}>{activeLost[0].petName} — tap for details</Text>
          </Pressable>
        ) : null}
      </View>

      <FlatList
        data={pins}
        keyExtractor={(pl, i) => pl.id ?? `place-${i}`}
        contentContainerStyle={s.listPad}
        ListEmptyComponent={places === null ? <LoadingState /> : <EmptyState title="No places found" description="Try another filter or area." />}
        renderItem={({ item: pl }) => (
          <Pressable style={s.row} onPress={() => setSelected(pl)}>
            <Text style={s.rowEmoji}>{PIN[pl.category ?? 'OTHER']}</Text>
            <View style={s.flex1}>
              <Text style={s.rowName} numberOfLines={1}>{pl.name} {pl.verifiedPartner ? '✓' : ''}</Text>
              <Text style={s.rowMeta} numberOfLines={1}>{pl.category?.toLowerCase().replace('_', ' ')}{pl.distanceMeters ? ` · ${(pl.distanceMeters / 1000).toFixed(1)} km` : ''}{pl.checkInsToday ? ` · ${pl.checkInsToday} check-ins` : ''}</Text>
            </View>
            {pl.rating ? <Badge label={`★ ${pl.rating.toFixed(1)}`} tone="warning" /> : null}
          </Pressable>
        )}
      />

      <Sheet visible={selected !== null} title={selected?.name ?? 'Place'} onClose={() => setSelected(null)}>
        {selected ? <PlaceBody place={selected} pets={pets} /> : null}
      </Sheet>
      <Sheet visible={alert !== null} title={alert ? `Lost pet: ${alert.petName}` : 'Lost pet'} onClose={() => setAlert(null)}>
        {alert ? <LostBody alert={alert} center={center} /> : null}
      </Sheet>
      <Sheet visible={mode === 'playdate'} title="Playdate now" onClose={() => setMode('none')}>
        <PlaydateBody pets={pets} center={center} onPet={(id) => { setMode('none'); router.push(`/pet/${id}`); }} />
      </Sheet>
      <Sheet visible={mode === 'lost'} title="Report lost pet" onClose={() => setMode('none')}>
        <ReportLostBody pets={pets} center={center} onDone={() => setMode('none')} />
      </Sheet>
    </View>
  );
}

function PlaceBody({ place, pets }: { place: Place; pets: Pet[] }) {
  const [done, setDone] = useState(false);
  const [petId, setPetId] = useState('');
  const pet = petId || pets[0]?.id;
  return (
    <View style={s.gap}>
      <View style={s.badgeRow}>
        <Badge label={place.category?.toLowerCase().replace('_', ' ') ?? 'place'} tone="accent" />
        {place.verifiedPartner ? <Badge label="Verified partner" tone="success" /> : null}
      </View>
      {place.address ? <Text style={s.bodyText}>📍 {place.address}</Text> : null}
      {place.description ? <Text style={s.bodyText}>{place.description}</Text> : null}
      {pets.length > 1 ? (
        <Field label="Check in as">
          <View style={s.chips}>{pets.map((p) => <Chip key={p.id} label={p.name} selected={pet === p.id} onPress={() => setPetId(p.id)} />)}</View>
        </Field>
      ) : null}
      {done ? <Text style={s.success}>Checked in! Enjoy your visit. 🐾</Text>
        : <Button label="Check in here" onPress={() => pet && place.id && checkIn(place.id, pet).then(() => setDone(true)).catch(() => {})} disabled={!pet} />}
    </View>
  );
}

function LostBody({ alert, center }: { alert: LostPetAlert; center: { lat: number; lng: number } }) {
  const [msg, setMsg] = useState('');
  const [sent, setSent] = useState(false);
  return (
    <View style={s.gap}>
      <Badge label={alert.status === 'ACTIVE' ? 'Active alert' : alert.status ?? ''} tone="danger" />
      <Text style={s.bodyText}>{alert.description}</Text>
      <Text style={s.metaText}>{alert.petSpecies} · seen {timeAgo(alert.lastSeenAt)} · {alert.sightings ?? 0} sightings</Text>
      {sent ? <Text style={s.success}>Thank you! The owner has been notified.</Text> : (
        <View style={s.gap}>
          <Field label="I have information"><Input value={msg} onChangeText={setMsg} placeholder="Where did you see this pet?" multiline /></Field>
          <Button label="Send sighting" disabled={!msg.trim()} onPress={() => alert.id && reportSighting(alert.id, { latitude: center.lat, longitude: center.lng, seenAt: new Date().toISOString(), note: msg }).then(() => setSent(true)).catch(() => {})} />
        </View>
      )}
    </View>
  );
}

function PlaydateBody({ pets, center, onPet }: { pets: Pet[]; center: { lat: number; lng: number }; onPet: (id: string) => void }) {
  const [nearby, setNearby] = useState<NearbyPet[] | null>(null);
  const [petId, setPetId] = useState('');
  const [sent, setSent] = useState<string | null>(null);
  useEffect(() => { getNearbyPets(center.lat, center.lng).then(setNearby).catch(() => setNearby([])); }, [center]);
  const mine = petId || pets[0]?.id;
  return (
    <View style={s.gap}>
      {pets.length > 1 ? <View style={s.chips}>{pets.map((p) => <Chip key={p.id} label={p.name} selected={mine === p.id} onPress={() => setPetId(p.id)} />)}</View> : null}
      {nearby === null ? <LoadingState label="Finding nearby pets…" /> : nearby.length === 0 ? (
        <Text style={s.bodyText}>No pets nearby right now. Try a busy park!</Text>
      ) : nearby.map((p) => (
        <View key={p.petId} style={s.row}>
          <Avatar name={p.petName ?? '?'} imageUrl={p.avatarUrl} size={40} />
          <View style={s.flex1}>
            <Text style={s.rowName} onPress={() => p.petId && onPet(p.petId)}>{p.petName}</Text>
            <Text style={s.rowMeta}>{p.species}{p.breed ? ` · ${p.breed}` : ''} · {Math.round(p.distanceMeters ?? 0)}m away</Text>
          </View>
          {sent === p.petId ? <Badge label="Invited!" tone="success" /> : (
            <Button label="Invite" variant="secondary" onPress={() => mine && p.petId && createPlaydate({ petId: mine, mode: 'NOW', inviteePetIds: [p.petId], latitude: center.lat, longitude: center.lng }).then(() => setSent(p.petId!)).catch(() => {})} />
          )}
        </View>
      ))}
    </View>
  );
}

function ReportLostBody({ pets, center, onDone }: { pets: Pet[]; center: { lat: number; lng: number }; onDone: () => void }) {
  const [petId, setPetId] = useState('');
  const [description, setDescription] = useState('');
  const [phone, setPhone] = useState('');
  const [radiusKm, setRadiusKm] = useState('2');
  const [done, setDone] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const pet = petId || pets[0]?.id;
  if (done) return <Text style={s.success}>Alert published. Nearby members will be notified.</Text>;
  return (
    <View style={s.gap}>
      {pets.length > 1 ? <Field label="Pet"><View style={s.chips}>{pets.map((p) => <Chip key={p.id} label={p.name} selected={pet === p.id} onPress={() => setPetId(p.id)} />)}</View></Field> : null}
      <Field label="Description"><Input value={description} onChangeText={setDescription} placeholder="Collar color, temperament, last seen…" multiline /></Field>
      <Field label="Contact phone"><Input value={phone} onChangeText={setPhone} keyboardType="phone-pad" /></Field>
      <Field label="Alert radius"><View style={s.chips}>{['1', '2', '5', '10'].map((r) => <Chip key={r} label={`${r} km`} selected={radiusKm === r} onPress={() => setRadiusKm(r)} />)}</View></Field>
      <ErrorText message={error} />
      <Button label="Publish alert" disabled={!pet || !description.trim()} onPress={() =>
        createLostPetAlert({ petId: pet ?? null, description, contactPhone: phone || undefined, latitude: center.lat, longitude: center.lng, lastSeenAt: new Date().toISOString(), radiusMeters: Number(radiusKm) * 1000 })
          .then(onDone).catch((e) => setError(e instanceof Error ? e.message : 'Could not publish'))} />
    </View>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  controls: { padding: 16, paddingBottom: 8, gap: 10 },
  search: { marginBottom: 0 },
  chips: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  actions: { flexDirection: 'row', gap: 10 },
  flex1: { flex: 1 },
  map: { margin: 16, marginTop: 8, height: 220, borderRadius: radius.card, backgroundColor: colors.tealLight, overflow: 'hidden', ...shadow.card },
  me: { position: 'absolute', left: '50%', top: '50%', marginLeft: -10, marginTop: -10 },
  meDot: { width: 20, height: 20, borderRadius: 10, backgroundColor: colors.teal, borderWidth: 4, borderColor: colors.white },
  pin: { position: 'absolute', width: 40, height: 40, borderRadius: 20, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center', ...shadow.card },
  pinLost: { borderWidth: 2, borderColor: colors.danger, backgroundColor: colors.dangerSoft },
  pinEmoji: { fontSize: 18 },
  lostBanner: { position: 'absolute', left: 10, right: 10, bottom: 10, backgroundColor: 'rgba(255,255,255,0.96)', borderRadius: radius.md, padding: 10 },
  lostTitle: { color: colors.danger, fontSize: fontSize.caption, fontFamily: fonts.w900 },
  lostBody: { color: colors.ink, fontSize: fontSize.label, fontFamily: fonts.w700, marginTop: 2 },
  listPad: { paddingHorizontal: 16, paddingBottom: 30 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.white, borderRadius: radius.md, padding: 12, marginBottom: 8, ...shadow.card },
  rowEmoji: { fontSize: 24 },
  rowName: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900 },
  rowMeta: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w700, marginTop: 2 },
  gap: { gap: 12 },
  badgeRow: { flexDirection: 'row', gap: 8, flexWrap: 'wrap' },
  bodyText: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w600 },
  metaText: { color: colors.muted, fontSize: fontSize.label, fontFamily: fonts.w700 },
  success: { color: colors.teal, backgroundColor: colors.tealLight, borderRadius: radius.sm, padding: 12, fontSize: fontSize.body, fontFamily: fonts.w800 },
});
