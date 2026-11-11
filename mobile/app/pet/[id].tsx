import { useEffect, useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useLocalSearchParams, useRouter } from 'expo-router';
import { createPlaydate, followPet, getHealthPassport, getPetAdventures, getPetBadges, getPetFriends, getPetPosts, getPetProfile, type Adventure, type BadgeDto, type HealthPassport, type PetFriend, type PetPost, type PetProfile } from '../../src/api/petlife';
import { listPets } from '../../src/api/client';
import type { Pet } from '../../src/types';
import { Avatar } from '../../src/petlife-ui';
import { Badge, Button, EmptyState, LoadingState } from '../../src/components';
import { colors, fonts, fontSize, radius, shadow } from '../../src/theme';

type Tab = 'posts' | 'adventures' | 'badges' | 'health' | 'friends';
const TABS: { key: Tab; label: string }[] = [
  { key: 'posts', label: 'Posts' }, { key: 'adventures', label: 'Adventures' }, { key: 'badges', label: 'Badges' }, { key: 'health', label: 'Health' }, { key: 'friends', label: 'Friends' },
];

export default function PetProfileScreen() {
  const { id: petId = '' } = useLocalSearchParams<{ id: string }>();
  const router = useRouter();
  const [profile, setProfile] = useState<PetProfile | null>(null);
  const [tab, setTab] = useState<Tab>('posts');
  const [posts, setPosts] = useState<PetPost[] | null>(null);
  const [adventures, setAdventures] = useState<Adventure[] | null>(null);
  const [badges, setBadges] = useState<BadgeDto[] | null>(null);
  const [health, setHealth] = useState<HealthPassport | null>(null);
  const [friends, setFriends] = useState<PetFriend[] | null>(null);
  const [following, setFollowing] = useState(false);
  const [invited, setInvited] = useState(false);
  const [myPets, setMyPets] = useState<Pet[]>([]);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    getPetProfile(petId).then((p) => { setProfile(p); setFollowing(Boolean(p.following)); }).catch(() => setFailed(true));
    listPets().then(setMyPets).catch(() => setMyPets([]));
  }, [petId]);

  useEffect(() => {
    if (tab === 'posts' && posts === null) getPetPosts(petId).then((r) => setPosts(r.items ?? [])).catch(() => setPosts([]));
    if (tab === 'adventures' && adventures === null) getPetAdventures(petId).then((r) => setAdventures(r.items ?? [])).catch(() => setAdventures([]));
    if (tab === 'badges' && badges === null) getPetBadges(petId).then(setBadges).catch(() => setBadges([]));
    if (tab === 'health' && health === null) getHealthPassport(petId).then(setHealth).catch(() => setHealth({ records: [], upcoming: [], weights: [] }));
    if (tab === 'friends' && friends === null) getPetFriends(petId).then((r) => setFriends(r.items ?? [])).catch(() => setFriends([]));
  }, [tab, petId]);

  if (failed) return <EmptyState title="Profile unavailable" description="This pet could not be loaded." />;
  if (!profile) return <LoadingState label="Loading profile…" />;
  const pet = profile.pet;
  if (!pet) return <EmptyState title="Profile unavailable" description="This pet could not be loaded." />;
  const eng = profile.engagement;
  const stats = profile.stats;
  const week = profile.weeklyActivity ?? [];
  const weekMax = Math.max(1, ...week.map((w) => w.minutes ?? 0));

  return (
    <ScrollView style={s.screen} contentContainerStyle={s.pad}>
      <View style={s.card}>
        <View style={s.cover}>
          {pet.coverUrl ? <Image source={{ uri: pet.coverUrl }} style={s.coverImg} /> : null}
          <Pressable onPress={() => router.back()} accessibilityLabel="Back" style={s.back}><Text style={s.backText}>‹</Text></Pressable>
        </View>
        <View style={s.avatarWrap}>
          {pet.avatarUrl ? <Image source={{ uri: pet.avatarUrl }} style={s.avatarImg} /> : <View style={s.avatarBox}><Text style={s.avatarPaw}>🐾</Text></View>}
        </View>
        <View style={s.body}>
          <View style={s.nameRow}>
            <View style={s.flex1}>
              <Text style={s.name} accessibilityRole="header">{pet.name}</Text>
              <Text style={s.meta}>{[pet.species, pet.breed, pet.birthDate ? `${new Date().getFullYear() - new Date(pet.birthDate).getFullYear()} yrs` : null].filter(Boolean).join(' · ')}</Text>
            </View>
          </View>
          {pet.bio ? <Text style={s.bio}>{pet.bio}</Text> : null}
          {!profile.own ? (
            <View style={s.actionRow}>
              <View style={s.flex1}><Button label={following ? 'Following' : 'Follow'} variant={following ? 'outline' : 'secondary'} onPress={() => followPet(petId, !following).then((st) => setFollowing(Boolean(st.following))).catch(() => {})} /></View>
              <View style={s.flex1}><Button label={invited ? '🐶 Invited!' : '🐶 Playdate'} disabled={myPets.length === 0} onPress={() => !invited && myPets[0] && createPlaydate({ petId: myPets[0].id, mode: 'NOW', inviteePetIds: [petId] }).then(() => setInvited(true)).catch(() => {})} /></View>
            </View>
          ) : null}

          <View style={s.statsRow}>
            {[[`${stats?.followers ?? 0}`, 'Followers'], [`${stats?.adventures ?? 0}`, 'Adventures'], [`${stats?.moments ?? 0}`, 'Moments']].map(([v, l]) => (
              <View key={l} style={s.stat}><Text style={s.statValue}>{v}</Text><Text style={s.statLabel}>{l}</Text></View>
            ))}
          </View>

          {eng ? (
            <View style={s.engBox}>
              <View style={s.rowBetween}>
                <Badge tone="warning" label={`⭐ Lv. ${eng.level} ${eng.levelTitle}`} />
                <Text style={s.xp}>{eng.xpIntoLevel}/{eng.xpForNextLevel ?? '∞'} XP</Text>
              </View>
              <View style={s.progress}><View style={[s.progressFill, { width: `${Math.min(100, ((eng.xpIntoLevel ?? 0) / Math.max(1, eng.xpForNextLevel ?? 1)) * 100)}%` }]} /></View>
              <View style={s.engMeta}>
                <Text style={s.engText}>🔥 {eng.streakDays ?? 0}-day streak</Text>
                <Text style={s.engText}>🏅 {eng.badgeCount ?? 0} badges</Text>
                <Text style={s.engText}>✨ {eng.xp ?? 0} XP</Text>
              </View>
            </View>
          ) : null}
        </View>
      </View>

      <ScrollView horizontal showsHorizontalScrollIndicator={false} contentContainerStyle={s.tabBar}>
        {TABS.map((t) => (
          <Pressable key={t.key} onPress={() => setTab(t.key)} accessibilityRole="tab" accessibilityState={{ selected: tab === t.key }} style={[s.tabBtn, tab === t.key && s.tabBtnOn]}>
            <Text style={[s.tabText, tab === t.key && s.tabTextOn]}>{t.label}</Text>
          </Pressable>
        ))}
      </ScrollView>

      {tab === 'posts' ? (
        posts === null ? <LoadingState /> : posts.length === 0 ? <EmptyState title="No posts yet" description="Share this pet's first moment!" /> : (
          <View style={s.grid}>
            {posts.map((p) => (
              <View key={p.postId} style={s.gridCell}>
                {p.media?.[0] ? <Image source={{ uri: p.media[0] }} style={s.gridImg} /> : <View style={s.gridText}><Text style={s.gridCaption} numberOfLines={4}>{p.caption}</Text></View>}
              </View>
            ))}
          </View>
        )
      ) : null}

      {tab === 'adventures' ? (
        adventures === null ? <LoadingState /> : adventures.length === 0 ? <EmptyState title="No adventures yet" description="Activities logged for this pet appear here." /> : (
          adventures.map((a) => (
            <View key={a.activityId} style={s.rowCard}>
              <Text style={s.rowEmoji}>🐾</Text>
              <View style={s.flex1}>
                <Text style={s.rowTitle}>{a.type?.replace('_', ' ')}</Text>
                <Text style={s.rowMeta}>{a.startedAt ? new Date(a.startedAt).toLocaleString() : ''}</Text>
              </View>
              <Text style={s.rowStat}>{Math.round((a.durationSeconds ?? 0) / 60)} min{a.distanceMeters ? ` · ${(a.distanceMeters / 1000).toFixed(1)} km` : ''}</Text>
            </View>
          ))
        )
      ) : null}

      {tab === 'badges' ? (
        badges === null ? <LoadingState /> : badges.length === 0 ? <EmptyState title="No badges yet" description="Keep exploring to earn the first medal!" /> : (
          <View style={s.grid}>
            {badges.map((b) => (
              <View key={b.code} style={[s.gridCell, s.badgeCell, !b.awarded && s.dim]}>
                <Text style={s.badgeEmoji}>{b.emoji ?? '🏅'}</Text>
                <Text style={s.badgeName} numberOfLines={1}>{b.name}</Text>
                <Text style={s.badgeMeta} numberOfLines={2}>{b.awarded ? 'Earned' : b.description}</Text>
              </View>
            ))}
          </View>
        )
      ) : null}

      {tab === 'health' ? (
        health === null ? <LoadingState /> : (
          <View style={s.card}>
            <Text style={s.sectionTitle} accessibilityRole="header">Health passport</Text>
            <Text style={s.meta}>Vaccination card · {health.records?.filter((r) => r.status === 'DONE').length ?? 0} of {health.records?.length ?? 0} up to date</Text>
            {(health.records ?? []).map((r) => (
              <View key={r.id} style={s.healthRow}>
                <Badge tone={r.status === 'DONE' ? 'success' : r.status === 'OVERDUE' ? 'danger' : 'warning'} label={r.status === 'DONE' ? '✓' : r.status === 'OVERDUE' ? '!' : '⏳'} />
                <View style={s.flex1}>
                  <Text style={s.rowTitle}>{r.name}</Text>
                  <Text style={s.rowMeta}>{r.type?.replace('_', ' ')}{r.administeredOn ? ` · given ${r.administeredOn}` : ''}{r.dueOn ? ` · due ${r.dueOn}` : ''}</Text>
                </View>
              </View>
            ))}
            {(health.records ?? []).length === 0 ? <Text style={s.meta}>No health records shared for this pet.</Text> : null}
            {(health.upcoming ?? []).map((r) => (
              <View key={r.id} style={s.upcomingRow}>
                <Text style={s.rowEmoji}>📅</Text>
                <View><Text style={s.rowTitle}>{r.name}</Text><Text style={s.rowMeta}>{r.clinicName ? `${r.clinicName} · ` : ''}{r.dueOn}</Text></View>
              </View>
            ))}
          </View>
        )
      ) : null}

      {tab === 'friends' ? (
        friends === null ? <LoadingState /> : friends.length === 0 ? <EmptyState title="No friends yet" description="Pets become friends when they follow each other." /> : (
          friends.map((f) => (
            <Pressable key={f.petId} style={s.rowCard} onPress={() => router.push(`/pet/${f.petId}`)}>
              <Avatar name={f.name ?? '?'} imageUrl={f.avatarUrl} size={44} />
              <View style={s.flex1}>
                <Text style={s.rowTitle}>{f.name}</Text>
                <Text style={s.rowMeta}>{f.species}{f.breed ? ` · ${f.breed}` : ''} · {f.ownerName}</Text>
              </View>
            </Pressable>
          ))
        )
      ) : null}

      <View style={s.card}>
        <Text style={s.sectionTitle} accessibilityRole="header">This week's activity</Text>
        <View style={s.chart}>
          {week.map((w) => (
            <View key={w.day} style={s.chartCol}>
              <View style={[s.chartBar, { height: Math.max(4, ((w.minutes ?? 0) / weekMax) * 100) }]} />
              <Text style={s.chartLbl}>{w.day}</Text>
            </View>
          ))}
        </View>
      </View>
    </ScrollView>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  pad: { padding: 16, paddingBottom: 40, gap: 12 },
  flex1: { flex: 1, minWidth: 0 },
  card: { backgroundColor: colors.white, borderRadius: radius.card, padding: 16, ...shadow.card },
  cover: { height: 110, backgroundColor: colors.tealLight, borderTopLeftRadius: radius.card, borderTopRightRadius: radius.card, overflow: 'hidden' },
  coverImg: { width: '100%', height: '100%' },
  back: { position: 'absolute', top: 10, left: 10, width: 36, height: 36, borderRadius: 18, backgroundColor: 'rgba(255,255,255,0.9)', alignItems: 'center', justifyContent: 'center' },
  backText: { color: colors.teal, fontSize: 24, marginTop: -4 },
  avatarWrap: { marginTop: -40, marginLeft: 16 },
  avatarImg: { width: 84, height: 84, borderRadius: 24, borderWidth: 4, borderColor: colors.cream },
  avatarBox: { width: 84, height: 84, borderRadius: 24, borderWidth: 4, borderColor: colors.cream, backgroundColor: colors.teal, alignItems: 'center', justifyContent: 'center' },
  avatarPaw: { fontSize: 34 },
  body: { padding: 16, paddingTop: 10 },
  nameRow: { flexDirection: 'row', alignItems: 'center' },
  name: { color: colors.tealDark, fontSize: fontSize.display, fontFamily: fonts.w900 },
  meta: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 2, textTransform: 'capitalize' },
  bio: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 8 },
  actionRow: { flexDirection: 'row', gap: 10, marginTop: 12 },
  statsRow: { flexDirection: 'row', gap: 8, marginTop: 14 },
  stat: { flex: 1, backgroundColor: colors.sand, borderRadius: radius.md, padding: 10, alignItems: 'center' },
  statValue: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900 },
  statLabel: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w800, marginTop: 2 },
  engBox: { backgroundColor: colors.sand, borderRadius: radius.md, padding: 12, marginTop: 10 },
  rowBetween: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  xp: { color: colors.muted, fontSize: fontSize.label, fontFamily: fonts.w800 },
  progress: { height: 10, borderRadius: 5, backgroundColor: colors.sandDeep, marginTop: 10, overflow: 'hidden' },
  progressFill: { height: '100%', backgroundColor: colors.teal, borderRadius: 5 },
  engMeta: { flexDirection: 'row', gap: 14, marginTop: 10 },
  engText: { color: colors.teal, fontSize: fontSize.label, fontFamily: fonts.w800 },
  tabBar: { flexDirection: 'row', gap: 6, backgroundColor: colors.white, borderRadius: radius.pill, padding: 4, ...shadow.card },
  tabBtn: { paddingHorizontal: 16, paddingVertical: 10, borderRadius: radius.pill },
  tabBtnOn: { backgroundColor: colors.teal },
  tabText: { color: colors.teal, fontFamily: fonts.w800, fontSize: fontSize.label },
  tabTextOn: { color: colors.white },
  grid: { flexDirection: 'row', flexWrap: 'wrap', gap: 8 },
  gridCell: { width: '31%', aspectRatio: 1, borderRadius: radius.md, overflow: 'hidden', backgroundColor: colors.white, ...shadow.card },
  gridImg: { width: '100%', height: '100%' },
  gridText: { flex: 1, backgroundColor: colors.tealLight, padding: 8, justifyContent: 'center' },
  gridCaption: { color: colors.teal, fontSize: fontSize.caption, fontFamily: fonts.w800, textAlign: 'center' },
  badgeCell: { padding: 10, alignItems: 'center', justifyContent: 'center' },
  badgeEmoji: { fontSize: 30 },
  badgeName: { color: colors.ink, fontSize: fontSize.caption, fontFamily: fonts.w900, marginTop: 4 },
  badgeMeta: { color: colors.muted, fontSize: 10, fontFamily: fonts.w600, textAlign: 'center' },
  dim: { opacity: 0.45 },
  rowCard: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.white, borderRadius: radius.md, padding: 12, marginBottom: 8, ...shadow.card },
  rowEmoji: { fontSize: 22 },
  rowTitle: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900, textTransform: 'capitalize' },
  rowMeta: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w700, marginTop: 2 },
  rowStat: { color: colors.teal, fontSize: fontSize.label, fontFamily: fonts.w800 },
  sectionTitle: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900, marginBottom: 4 },
  healthRow: { flexDirection: 'row', alignItems: 'center', gap: 10, backgroundColor: colors.sand, borderRadius: radius.sm, padding: 10, marginTop: 8 },
  upcomingRow: { flexDirection: 'row', alignItems: 'center', gap: 10, backgroundColor: colors.amberSoft, borderRadius: radius.sm, padding: 10, marginTop: 8 },
  chart: { flexDirection: 'row', alignItems: 'flex-end', gap: 6, height: 110, marginTop: 12 },
  chartCol: { flex: 1, alignItems: 'center', justifyContent: 'flex-end', gap: 4 },
  chartBar: { width: '100%', backgroundColor: colors.teal, borderTopLeftRadius: 4, borderTopRightRadius: 4 },
  chartLbl: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w800 },
});
