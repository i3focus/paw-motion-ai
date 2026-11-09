import type { ReactNode } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';
import { Badge } from './components';
import type { Activity, Pet } from './types';
import { colors } from './theme';

const dogPortrait = require('../assets/dog-portrait.jpg');
const dogWalk = require('../assets/dog-walk.jpg');

export function PetAvatar({ pet, size = 64 }: { pet: Pet; size?: number }) {
  if (pet.species === 'dog') {
    return (
      <View style={[styles.avatar, { width: size, height: size, borderRadius: size * 0.35 }]}>
        <Image source={dogPortrait} style={styles.avatarImage} resizeMode="cover" />
        <View style={styles.avatarPaw}><Text style={styles.avatarPawText}>Paw</Text></View>
      </View>
    );
  }
  return (
    <View style={[styles.avatarFallback, { width: size, height: size, borderRadius: size * 0.35 }]}>
      <Text style={styles.avatarFallbackText}>{pet.species.slice(0, 1).toUpperCase()}</Text>
    </View>
  );
}

export function PetCard({ pet, action }: { pet: Pet; action?: ReactNode }) {
  return (
    <View style={styles.petCard}>
      <View style={styles.petTop}>
        <PetAvatar pet={pet} size={72} />
        <View style={styles.petBody}>
          <View style={styles.petTitleRow}>
            <Text style={styles.petName}>{pet.name}</Text>
            <Badge label={pet.status} tone="accent" />
          </View>
          <Text style={styles.petMeta}>{pet.species} · {pet.size}</Text>
          {pet.breed ? <Text style={styles.petDetail}>{pet.breed}</Text> : null}
          {pet.activityPreference ? <Text style={styles.petDetail}>Preference: {pet.activityPreference}</Text> : null}
          {pet.birthDate ? <Text style={styles.petDetail}>Born {pet.birthDate}</Text> : null}
        </View>
      </View>
      {action ? <View style={styles.petAction}>{action}</View> : null}
    </View>
  );
}

export function ActivityCard({ activity, petName, compact = false }: { activity: Activity; petName?: string; compact?: boolean }) {
  const minutes = Math.round(activity.durationSeconds / 60);
  const distance = activity.distanceMeters ? `${(activity.distanceMeters / 1000).toFixed(2)} km` : 'No distance';
  if (compact) {
    return (
      <View style={styles.activityCompact}>
        <View style={styles.activityIcon}><Text style={styles.activityIconText}>Paw</Text></View>
        <View style={styles.activityBody}>
          <View style={styles.activityTitleRow}>
            <Text style={styles.activityTitle}>{activity.type.replace('_', ' ')}</Text>
            {petName ? <Badge label={petName} /> : null}
          </View>
          <Text style={styles.activityMeta}>{new Date(activity.startedAt).toLocaleString()}</Text>
          <Text style={styles.activityStats}>{minutes} min · {distance}</Text>
        </View>
      </View>
    );
  }
  return (
    <View style={styles.activityCard}>
      <View style={styles.activityImageWrap}>
        <Image source={dogWalk} style={styles.activityImage} resizeMode="cover" />
        <View style={styles.activityImageOverlay} />
        <View style={styles.activityImageBadge}><Badge label={activity.type.replace('_', ' ')} tone="accent" /></View>
        <Text style={styles.activityImageTitle}>{petName ? `${petName}'s activity` : 'Pet activity'}</Text>
        <Text style={styles.activityImageDate}>{new Date(activity.startedAt).toLocaleString()}</Text>
      </View>
      <View style={styles.activityStatsGrid}>
        <View style={styles.activityStat}><Text style={styles.activityStatLabel}>Duration</Text><Text style={styles.activityStatValue}>{minutes} min</Text></View>
        <View style={styles.activityStat}><Text style={styles.activityStatLabel}>Distance</Text><Text style={styles.activityStatValue}>{activity.distanceMeters ? `${(activity.distanceMeters / 1000).toFixed(1)} km` : '—'}</Text></View>
        <View style={styles.activityStat}><Text style={styles.activityStatLabel}>Pet</Text><Text style={styles.activityStatValue}>{petName ?? 'Pet'}</Text></View>
      </View>
      {activity.notes ? <Text style={styles.activityNotes}>{activity.notes}</Text> : null}
    </View>
  );
}

const styles = StyleSheet.create({
  avatar: { overflow: 'hidden', backgroundColor: colors.cream100, borderWidth: 1, borderColor: colors.cream200 },
  avatarImage: { width: '100%', height: '100%' },
  avatarPaw: { position: 'absolute', right: 4, bottom: 4, backgroundColor: colors.white, borderRadius: 10, paddingHorizontal: 6, paddingVertical: 2 },
  avatarPawText: { color: colors.primary, fontSize: 10, fontWeight: '900' },
  avatarFallback: { backgroundColor: colors.accentSoft, alignItems: 'center', justifyContent: 'center', borderWidth: 1, borderColor: colors.cream200 },
  avatarFallbackText: { color: colors.primary, fontSize: 22, fontWeight: '900' },
  petCard: { backgroundColor: colors.white, borderColor: colors.cream200, borderWidth: 1, borderRadius: 24, padding: 16 },
  petTop: { flexDirection: 'row', gap: 14 },
  petBody: { flex: 1 },
  petTitleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  petName: { color: colors.cream900, fontSize: 18, fontWeight: '900', textTransform: 'capitalize', flex: 1 },
  petMeta: { color: colors.cream600, fontSize: 14, marginTop: 4, textTransform: 'capitalize' },
  petDetail: { color: colors.cream700, fontSize: 14, marginTop: 4 },
  petAction: { marginTop: 14 },
  activityCard: { backgroundColor: colors.white, borderColor: colors.cream200, borderWidth: 1, borderRadius: 24, overflow: 'hidden' },
  activityImageWrap: { height: 150 },
  activityImage: { width: '100%', height: '100%' },
  activityImageOverlay: { position: 'absolute', top: 0, left: 0, right: 0, bottom: 0, backgroundColor: 'rgba(54,39,20,0.28)' },
  activityImageBadge: { position: 'absolute', left: 12, top: 12 },
  activityImageTitle: { position: 'absolute', left: 12, right: 12, bottom: 22, color: colors.white, fontSize: 20, fontWeight: '900' },
  activityImageDate: { position: 'absolute', left: 12, right: 12, bottom: 8, color: 'rgba(255,249,240,0.85)', fontSize: 12 },
  activityStatsGrid: { flexDirection: 'row', gap: 10, padding: 14 },
  activityStat: { flex: 1, backgroundColor: colors.cream50, borderRadius: 16, padding: 10 },
  activityStatLabel: { color: colors.cream500, fontSize: 11, fontWeight: '800', textTransform: 'uppercase' },
  activityStatValue: { color: colors.cream900, fontSize: 15, fontWeight: '900', marginTop: 4 },
  activityNotes: { color: colors.cream600, fontSize: 14, paddingHorizontal: 14, paddingBottom: 14 },
  activityCompact: { flexDirection: 'row', gap: 12, backgroundColor: colors.cream50, borderColor: colors.cream200, borderWidth: 1, borderRadius: 20, padding: 12, alignItems: 'center' },
  activityIcon: { width: 44, height: 44, borderRadius: 14, backgroundColor: colors.accentSoft, alignItems: 'center', justifyContent: 'center' },
  activityIconText: { color: colors.primary, fontSize: 11, fontWeight: '900' },
  activityBody: { flex: 1 },
  activityTitleRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', gap: 8 },
  activityTitle: { color: colors.cream900, fontSize: 15, fontWeight: '800', textTransform: 'capitalize' },
  activityMeta: { color: colors.cream500, fontSize: 13, marginTop: 3 },
  activityStats: { color: colors.cream700, fontSize: 13, fontWeight: '700', marginTop: 3 },
});
