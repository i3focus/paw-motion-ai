import { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, getDashboardSummary, listPets, listRecommendations } from '../api/client';
import { Badge, Card, EmptyState, ErrorText } from '../components';
import { ActivityCard } from '../petVisuals';
import type { DashboardSummary, OwnerRecommendation, Pet } from '../types';
import { colors, fonts } from '../theme';

export function DashboardScreen() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [recommendations, setRecommendations] = useState<OwnerRecommendation[]>([]);
  const [pets, setPets] = useState<Pet[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [dashboard, recs, petList] = await Promise.all([
        getDashboardSummary(),
        listRecommendations().catch(() => []),
        listPets().catch(() => []),
      ]);
      setSummary(dashboard);
      setRecommendations(recs);
      setPets(petList);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load dashboard');
    }
  }, []);

  useEffect(() => {
    load();
  }, [load]);

  return (
    <ScrollView style={styles.screen} contentContainerStyle={styles.content} refreshControl={<RefreshControl refreshing={refreshing} onRefresh={async () => { setRefreshing(true); await load(); setRefreshing(false); }} />}>
      <Text style={styles.title}>Dashboard</Text>
      <Text style={styles.subtitle}>Your pet activity overview for the last 7 days.</Text>
      <ErrorText message={error} />
      {summary ? (
        <View style={styles.stats}>
          <Stat label="Pets" value={String(summary.petCount)} />
          <Stat label="Activities" value={String(summary.activityCountLast7Days)} />
          <Stat label="Distance" value={`${(summary.totalDistanceMetersLast7Days / 1000).toFixed(1)} km`} />
          <Stat label="Duration" value={`${Math.round(summary.totalDurationSecondsLast7Days / 60)} min`} />
          <Stat label="Streak" value={`${summary.currentStreakDays} days`} />
        </View>
      ) : null}
      <Card title="Recent activities">
        {summary?.recentActivities.length ? (
          <View style={styles.activityList}>
            {summary.recentActivities.map((activity) => {
              const pet = pets.find((candidate) => candidate.id === activity.petId);
              return <ActivityCard key={activity.id} activity={activity} petName={pet?.name} compact />;
            })}
          </View>
        ) : <EmptyState title="No activities yet" description="Record a walk to see recent activity here." />}
      </Card>
      <Card title="Partner recommendations">
        {recommendations.length ? recommendations.map((recommendation) => (
          <View key={recommendation.campaignId} style={styles.recommendation}>
            <View style={styles.rowHeader}>
              <Text style={styles.rowTitle}>{recommendation.campaignTitle}</Text>
              <Badge label="AI topics" tone="accent" />
            </View>
            <Text style={styles.rowSubtitle}>{recommendation.partnerName}</Text>
            <Text style={styles.recommendationText}>{recommendation.description}</Text>
            <View style={styles.topicRow}>
              {recommendation.recommendedTopics.map((topic) => <Badge key={topic} label={topic} />)}
            </View>
          </View>
        )) : <EmptyState title="No recommendations yet" description="AI-analyzed partner campaigns will appear here." />}
      </Card>
    </ScrollView>
  );
}

function Stat({ label, value }: { label: string; value: string }) {
  return <View style={styles.stat}><Text style={styles.statLabel}>{label}</Text><Text style={styles.statValue}>{value}</Text></View>;
}

const styles = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  content: { padding: 20, gap: 16 },
  title: { color: colors.ink, fontSize: 30, fontFamily: fonts.w900 },
  subtitle: { color: colors.muted, marginTop: -10, marginBottom: 4 },
  stats: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  stat: { flexGrow: 1, flexBasis: '30%', backgroundColor: colors.white, borderColor: colors.line, borderWidth: 1, borderRadius: 22, padding: 14 },
  statLabel: { color: colors.muted, fontSize: 12, fontFamily: fonts.w700 },
  statValue: { color: colors.ink, fontSize: 24, fontFamily: fonts.w900, marginTop: 6 },
  activityList: { gap: 10 },
  rowHeader: { flexDirection: 'row', justifyContent: 'space-between', gap: 8 },
  rowTitle: { color: colors.ink, fontFamily: fonts.w800 },
  rowSubtitle: { color: colors.muted, fontSize: 13, marginTop: 3 },
  recommendation: { borderTopColor: colors.line, borderTopWidth: 1, paddingTop: 12, marginTop: 4 },
  recommendationText: { color: colors.muted, fontSize: 14, marginTop: 8, lineHeight: 20 },
  topicRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 10 },
});
