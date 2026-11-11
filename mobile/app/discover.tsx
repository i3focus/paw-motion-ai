import { useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import * as Location from 'expo-location';
import { getChallenges, getDiscover, getLeaderboard, joinChallenge, joinCommunity, rsvpEvent, type Challenge, type Discover, type Leaderboard } from '../src/api/petlife';
import { listPets } from '../src/api/client';
import type { Pet } from '../src/types';
import { Avatar } from '../src/petlife-ui';
import { Badge, Button, Chip, ErrorText, LoadingState, SponsoredLabel } from '../src/components';
import { colors, fonts, fontSize, radius, shadow } from '../src/theme';

export default function DiscoverScreen() {
  const router = useRouter();
  const [discover, setDiscover] = useState<Discover | null>(null);
  const [board, setBoard] = useState<Leaderboard | null>(null);
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [pets, setPets] = useState<Pet[]>([]);
  const [scope, setScope] = useState<'neighborhood' | 'global'>('neighborhood');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    Location.getLastKnownPositionAsync()
      .then((pos) => getDiscover(pos?.coords.latitude, pos?.coords.longitude))
      .then(setDiscover)
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not load Discover'));
    listPets().then(setPets).catch(() => setPets([]));
    getChallenges().then(setChallenges).catch(() => setChallenges([]));
  }, []);
  useEffect(() => { getLeaderboard(scope).then(setBoard).catch(() => setBoard({ entries: [] })); }, [scope]);

  if (!discover && !error) return <LoadingState label="Loading Discover…" />;

  return (
    <ScrollView style={s.screen} contentContainerStyle={s.content}>
      <Text style={s.h1} accessibilityRole="header">Discover</Text>
      <ErrorText message={error} />

      {(discover?.sponsored ?? []).length > 0 ? (
        <View>
          <Text style={s.h2}>Near you</Text>
          {discover!.sponsored!.map((sp) => (
            <View key={sp.campaignId ?? sp.partnerId} style={[s.card, s.sponsored]}>
              <View style={s.rowBetween}><SponsoredLabel />{sp.verified ? <Badge label="✓ Verified" tone="success" /> : null}</View>
              <Text style={s.cardTitle}>{sp.title ?? sp.partnerName}</Text>
              <Text style={s.cardText}>{sp.description}</Text>
            </View>
          ))}
        </View>
      ) : null}

      <View>
        <Text style={s.h2}>Communities</Text>
        {(discover?.communities ?? []).map((c) => <CommunityCard key={c.id} community={c} />)}
        {(discover?.communities ?? []).length === 0 ? <Text style={s.muted}>No communities yet — start one for your breed or neighborhood!</Text> : null}
      </View>

      <View>
        <Text style={s.h2}>Upcoming events</Text>
        {(discover?.events ?? []).map((e) => (
          <View key={e.id} style={s.card}>
            <Text style={s.cardTitle}>{e.title}</Text>
            <Text style={s.cardMeta}>{e.communityName ? `${e.communityName} · ` : ''}{e.locationName ?? ''}</Text>
            <Text style={s.cardMeta}>{e.startsAt ? new Date(e.startsAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''} · {e.goingCount ?? 0} going</Text>
            {e.viewerRsvp ? <Badge tone="success" label={e.viewerRsvp === 'GOING' ? 'Going ✓' : 'Interested ✓'} /> : (
              <View style={s.btnRow}>
                <View style={s.flex1}><Button label="Going" onPress={() => rsvpEvent(e.id!, 'GOING').then(() => setDiscover({ ...discover!, events: discover!.events?.map((x) => (x.id === e.id ? { ...x, viewerRsvp: 'GOING' } : x)) })).catch(() => {})} /></View>
                <View style={s.flex1}><Button label="Interested" variant="ghost" onPress={() => rsvpEvent(e.id!, 'INTERESTED').then(() => setDiscover({ ...discover!, events: discover!.events?.map((x) => (x.id === e.id ? { ...x, viewerRsvp: 'INTERESTED' } : x)) })).catch(() => {})} /></View>
              </View>
            )}
          </View>
        ))}
        {(discover?.events ?? []).length === 0 ? <Text style={s.muted}>No upcoming events in your communities.</Text> : null}
      </View>

      <View>
        <Text style={s.h2}>Weekly challenges</Text>
        {challenges.map((c) => (
          <View key={c.id} style={s.card}>
            <View style={s.rowBetween}><Text style={s.cardTitle}>{c.title}</Text><Badge label={`+${c.xpReward} XP`} tone="warning" /></View>
            <Text style={s.cardText}>{c.description}</Text>
            <Text style={s.cardMeta}>{c.metric?.replace('_', ' ').toLowerCase()} goal: {c.targetValue}{c.endsOn ? ` · until ${c.endsOn}` : ''}</Text>
            <Button label={(c.joined ?? []).length > 0 ? 'Joined ✓' : 'Join challenge'} disabled={(c.joined ?? []).length > 0 || pets.length === 0}
              onPress={() => pets[0] && joinChallenge(c.id!, pets[0].id).then((u) => setChallenges((l) => l.map((x) => (x.id === u.id ? u : x)))).catch(() => {})} />
          </View>
        ))}
        {challenges.length === 0 ? <Text style={s.muted}>No active challenges this week.</Text> : null}
      </View>

      <View>
        <View style={s.rowBetween}>
          <Text style={s.h2}>Leaderboard</Text>
          <View style={s.btnRow}>
            <Chip label="Nearby" selected={scope === 'neighborhood'} onPress={() => setScope('neighborhood')} />
            <Chip label="Global" selected={scope === 'global'} onPress={() => setScope('global')} />
          </View>
        </View>
        <View style={s.card}>
          {(board?.entries ?? []).map((e) => (
            <Pressable key={e.petId} style={[s.lbRow, e.own && s.lbOwn]} onPress={() => e.petId && router.push(`/pet/${e.petId}`)}>
              <Text style={s.lbRank}>{e.rank === 1 ? '🥇' : e.rank === 2 ? '🥈' : e.rank === 3 ? '🥉' : e.rank}</Text>
              <Avatar name={e.petName ?? '?'} imageUrl={e.petAvatarUrl} size={36} />
              <View style={s.flex1}>
                <Text style={s.cardTitle2}>{e.petName}{e.own ? ' (you)' : ''}</Text>
                <Text style={s.cardMeta}>Lv. {e.level} {e.levelTitle}</Text>
              </View>
              <Text style={s.lbXp}>{e.weeklyXp} XP</Text>
            </Pressable>
          ))}
          {(board?.entries ?? []).length === 0 ? <Text style={s.muted}>Log an activity to climb the ranks!</Text> : null}
        </View>
      </View>
    </ScrollView>
  );
}

function CommunityCard({ community: c }: { community: NonNullable<Discover['communities']>[number] }) {
  const [joined, setJoined] = useState(Boolean(c.joined));
  return (
    <View style={s.card}>
      <Badge label={c.kind?.toLowerCase() ?? 'community'} tone="accent" />
      <Text style={s.cardTitle}>{c.name}</Text>
      <Text style={s.cardText} numberOfLines={2}>{c.description}</Text>
      <Text style={s.cardMeta}>{c.memberCount} members{c.upcomingEvents ? ` · ${c.upcomingEvents} events` : ''}</Text>
      <Button label={joined ? 'Joined ✓' : 'Join'} variant={joined ? 'outline' : 'secondary'}
        onPress={() => joinCommunity(c.id!, !joined).then((r) => setJoined(Boolean(r.joined))).catch(() => {})} />
    </View>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  content: { padding: 16, paddingBottom: 40, gap: 18 },
  flex1: { flex: 1 },
  h1: { color: colors.tealDark, fontSize: fontSize.display, fontFamily: fonts.w900 },
  h2: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900, marginBottom: 10 },
  card: { backgroundColor: colors.white, borderRadius: radius.card, padding: 14, marginBottom: 10, gap: 6, ...shadow.card },
  sponsored: { borderColor: colors.amber, borderWidth: 2 },
  cardTitle: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900 },
  cardTitle2: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900 },
  cardText: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600 },
  cardMeta: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w700 },
  muted: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600 },
  rowBetween: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  btnRow: { flexDirection: 'row', gap: 8 },
  lbRow: { flexDirection: 'row', alignItems: 'center', gap: 10, paddingVertical: 8 },
  lbOwn: { backgroundColor: colors.tealLight, borderRadius: radius.sm, paddingHorizontal: 8 },
  lbRank: { width: 30, textAlign: 'center', color: colors.tealDark, fontSize: fontSize.bodyLg, fontFamily: fonts.w900 },
  lbXp: { color: colors.teal, fontSize: fontSize.label, fontFamily: fonts.w900 },
});
