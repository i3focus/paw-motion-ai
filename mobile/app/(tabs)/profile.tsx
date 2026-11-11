import { useCallback, useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Switch, Text, View } from 'react-native';
import { useFocusEffect, useRouter } from 'expo-router';
import { useAuth } from '../../src/auth';
import { createPet, listPets, updateProfile } from '../../src/api/client';
import { getNotificationPreferences, getUnreadCount, updateNotificationPreferences, type NotificationPreferences } from '../../src/api/petlife';
import type { Pet } from '../../src/types';
import { Avatar, Sheet } from '../../src/petlife-ui';
import { PetAvatar } from '../../src/petVisuals';
import { Badge, Button, Card, ErrorText, Field, Input, LoadingState } from '../../src/components';
import { colors, fonts, fontSize, radius, shadow } from '../../src/theme';

const PREFS: { key: keyof NotificationPreferences; label: string; hint: string }[] = [
  { key: 'healthReminders', label: 'Health reminders', hint: 'Vaccines and vet visits coming due' },
  { key: 'lostPets', label: 'Lost pets nearby', hint: 'Alerts within your area' },
  { key: 'playdates', label: 'Playdates', hint: 'Invites and responses' },
  { key: 'social', label: 'Social', hint: 'Reactions, comments, follows, badges' },
  { key: 'bookings', label: 'Bookings', hint: 'Reservation updates from partners' },
];

export default function ProfileScreen() {
  const { owner, logout, refreshProfile } = useAuth();
  const router = useRouter();
  const [pets, setPets] = useState<Pet[] | null>(null);
  const [prefs, setPrefs] = useState<NotificationPreferences | null>(null);
  const [unread, setUnread] = useState(0);
  const [sheet, setSheet] = useState<'none' | 'pet' | 'edit'>('none');
  const [petForm, setPetForm] = useState({ name: '', species: 'dog', breed: '', size: 'medium' });
  const [name, setName] = useState(owner?.displayName ?? '');
  const [bio, setBio] = useState(owner?.bio ?? '');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useFocusEffect(useCallback(() => {
    listPets().then(setPets).catch(() => setPets([]));
    getUnreadCount().then((c) => setUnread(c.unread)).catch(() => setUnread(0));
  }, []));
  useEffect(() => { getNotificationPreferences().then(setPrefs).catch(() => setPrefs({})); }, []);

  return (
    <ScrollView style={s.screen} contentContainerStyle={s.content}>
      <Card>
        <View style={s.headRow}>
          <Avatar name={owner?.displayName ?? '?'} imageUrl={owner?.avatarUrl} size={64} />
          <View style={s.flex1}>
            <Text style={s.name} accessibilityRole="header">{owner?.displayName}</Text>
            <Text style={s.meta}>{owner?.email}</Text>
          </View>
          <Button label="Edit" variant="outline" onPress={() => { setName(owner?.displayName ?? ''); setBio(owner?.bio ?? ''); setSheet('edit'); }} />
        </View>
      </Card>

      <Pressable style={s.notifBtn} onPress={() => router.push('/notifications')}>
        <Text style={s.notifText}>🔔 Notifications</Text>
        {unread > 0 ? <Badge label={`${unread} new`} tone="accent" /> : <Text style={s.notifChevron}>›</Text>}
      </Pressable>
      <Pressable style={s.notifBtn} onPress={() => router.push('/discover')}>
        <Text style={s.notifText}>🧭 Discover</Text>
        <Text style={s.notifChevron}>›</Text>
      </Pressable>

      <View style={s.sectionHead}>
        <Text style={s.sectionTitle}>My pets</Text>
        <Button label="+ Add" variant="secondary" onPress={() => setSheet('pet')} />
      </View>
      {pets === null ? <LoadingState /> : (
        <View style={s.petGrid}>
          {pets.map((p) => (
            <Pressable key={p.id} style={s.petCard} onPress={() => router.push(`/pet/${p.id}`)}>
              <PetAvatar pet={p} size={56} />
              <Text style={s.petName} numberOfLines={1}>{p.name}</Text>
              <Text style={s.petMeta} numberOfLines={1}>{p.species}{p.breed ? ` · ${p.breed}` : ''}</Text>
            </Pressable>
          ))}
          <Pressable style={[s.petCard, s.petAdd]} onPress={() => setSheet('pet')}>
            <Text style={s.petAddIcon}>🐾</Text>
            <Text style={s.petAddText}>Add pet</Text>
          </Pressable>
        </View>
      )}

      <Card title="Notifications">
        {PREFS.map((row) => (
          <View key={row.key} style={s.prefRow}>
            <View style={s.flex1}>
              <Text style={s.prefLabel}>{row.label}</Text>
              <Text style={s.prefHint}>{row.hint}</Text>
            </View>
            <Switch value={Boolean(prefs?.[row.key] ?? true)} disabled={prefs === null} trackColor={{ true: colors.teal, false: colors.sandDeep }} thumbColor={colors.white}
              onValueChange={(v) => {
                const next = { ...(prefs ?? {}), [row.key]: v };
                setPrefs(next);
                updateNotificationPreferences(next).catch(() => {});
              }} />
          </View>
        ))}
      </Card>

      <Button label="Sign out" variant="outline" onPress={logout} />

      <Sheet visible={sheet === 'pet'} title="Add a pet" onClose={() => setSheet('none')}>
        <View style={s.gap}>
          <Field label="Name"><Input value={petForm.name} onChangeText={(v) => setPetForm({ ...petForm, name: v })} /></Field>
          <Field label="Species"><View style={s.chips}>{['dog', 'cat', 'bird', 'rabbit', 'other'].map((sp) => <Pressable key={sp} onPress={() => setPetForm({ ...petForm, species: sp })} style={[s.opt, petForm.species === sp && s.optOn]}><Text style={[s.optText, petForm.species === sp && s.optTextOn]}>{sp}</Text></Pressable>)}</View></Field>
          <Field label="Breed (optional)"><Input value={petForm.breed} onChangeText={(v) => setPetForm({ ...petForm, breed: v })} /></Field>
          <Field label="Size"><View style={s.chips}>{['small', 'medium', 'large'].map((sz) => <Pressable key={sz} onPress={() => setPetForm({ ...petForm, size: sz })} style={[s.opt, petForm.size === sz && s.optOn]}><Text style={[s.optText, petForm.size === sz && s.optTextOn]}>{sz}</Text></Pressable>)}</View></Field>
          <ErrorText message={error} />
          <Button label={busy ? 'Saving…' : 'Add pet'} disabled={busy || petForm.name.trim().length < 2} onPress={() => {
            setBusy(true); setError(null);
            createPet({ name: petForm.name.trim(), species: petForm.species, breed: petForm.breed.trim() || undefined, size: petForm.size })
              .then(() => { setSheet('none'); setPetForm({ name: '', species: 'dog', breed: '', size: 'medium' }); return listPets().then(setPets); })
              .catch((e) => setError(e instanceof Error ? e.message : 'Could not add pet'))
              .finally(() => setBusy(false));
          }} />
        </View>
      </Sheet>

      <Sheet visible={sheet === 'edit'} title="Edit profile" onClose={() => setSheet('none')}>
        <View style={s.gap}>
          <Field label="Display name"><Input value={name} onChangeText={setName} /></Field>
          <Field label="Bio"><Input value={bio} onChangeText={setBio} multiline /></Field>
          <ErrorText message={error} />
          <Button label={busy ? 'Saving…' : 'Save'} disabled={busy} onPress={() => {
            setBusy(true); setError(null);
            updateProfile({ displayName: name.trim() || undefined, bio: bio.trim() || undefined })
              .then(() => refreshProfile().then(() => setSheet('none')))
              .catch((e) => setError(e instanceof Error ? e.message : 'Could not save'))
              .finally(() => setBusy(false));
          }} />
        </View>
      </Sheet>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  content: { padding: 16, paddingBottom: 40, gap: 14 },
  flex1: { flex: 1, minWidth: 0 },
  headRow: { flexDirection: 'row', alignItems: 'center', gap: 12 },
  name: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900 },
  meta: { color: colors.muted, fontSize: fontSize.label, fontFamily: fonts.w600 },
  notifBtn: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', backgroundColor: colors.white, borderRadius: radius.md, padding: 14, ...shadow.card },
  notifText: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900 },
  notifChevron: { color: colors.muted, fontSize: 20, fontFamily: fonts.w800 },
  sectionHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginTop: 6 },
  sectionTitle: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900 },
  petGrid: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  petCard: { width: '47%', backgroundColor: colors.white, borderRadius: radius.card, padding: 14, alignItems: 'center', gap: 6, ...shadow.card },
  petAdd: { backgroundColor: colors.sand, justifyContent: 'center', minHeight: 140 },
  petAddIcon: { fontSize: 28 },
  petAddText: { color: colors.teal, fontFamily: fonts.w900, fontSize: fontSize.label },
  petName: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900, textTransform: 'capitalize' },
  petMeta: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w700, textTransform: 'capitalize' },
  prefRow: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 10, borderBottomColor: colors.line, borderBottomWidth: 1 },
  prefLabel: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900 },
  prefHint: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w600 },
  gap: { gap: 12 },
  chips: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  opt: { borderRadius: radius.pill, paddingHorizontal: 16, paddingVertical: 10, backgroundColor: colors.white, borderWidth: 2, borderColor: colors.line },
  optOn: { borderColor: colors.teal, backgroundColor: colors.tealLight },
  optText: { color: colors.teal, fontFamily: fonts.w800, fontSize: fontSize.label, textTransform: 'capitalize' },
  optTextOn: { color: colors.tealDark },
});
