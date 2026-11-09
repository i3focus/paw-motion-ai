import { useCallback, useEffect, useState } from 'react';
import { RefreshControl, ScrollView, StyleSheet, Text, View } from 'react-native';
import { ApiError, getDashboardSummary, listRecommendations } from '../api/client';
import { Badge, Card, EmptyState, ErrorText } from '../components';
import type { DashboardSummary, OwnerRecommendation } from '../types';
import { colors } from '../theme';

export function DashboardScreen() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [recommendations, setRecommendations] = useState<OwnerRecommendation[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const load = useCallback(async () => {
    setError(null);
    try {
      const [dashboard, recs] = await Promise.all([getDashboardSummary(), listRecommendations().catch(() => [])]);
      setSummary(dashboard);
      setRecommendations(recs);
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
        {summary?.recentActivities.length ? summary.recentActivities.map((activity) => (
          <View key={activity.id} style={styles.row}>
            <Badge label={activity.type.replace('_', ' ')} tone="accent" />
            <View style={styles.rowBody}>
              <Text style={styles.rowTitle}>{new Date(activity.startedAt).toLocaleString()}</Text>
              <Text style={styles.rowSubtitle}>{Math.round(activity.durationSeconds / 60)} min{activity.distanceMeters ? ` · ${(activity.distanceMeters / 1000).toFixed(2)} km` : ''}</Text>
            </View>
          </View>
        )) : <EmptyState title="No activities yet" description="Record a walk to see recent activity here." />}
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
  screen: { flex: 1, backgroundColor: colors.cream50 },
  content: { padding: 20, gap: 16 },
  title: { color: colors.cream900, fontSize: 30, fontWeight: '900' },
  subtitle: { color: colors.cream600, marginTop: -10, marginBottom: 4 },
  stats: { flexDirection: 'row', flexWrap: 'wrap', gap: 10 },
  stat: { flexGrow: 1, flexBasis: '30%', backgroundColor: colors.white, borderColor: colors.cream200, borderWidth: 1, borderRadius: 22, padding: 14 },
  statLabel: { color: colors.cream500, fontSize: 12, fontWeight: '700' },
  statValue: { color: colors.cream900, fontSize: 24, fontWeight: '900', marginTop: 6 },
  row: { flexDirection: 'row', alignItems: 'center', gap: 12, paddingVertical: 8 },
  rowHeader: { flexDirection: 'row', justifyContent: 'space-between', gap: 8 },
  rowBody: { flex: 1 },
  rowTitle: { color: colors.cream900, fontWeight: '800' },
  rowSubtitle: { color: colors.cream500, fontSize: 13, marginTop: 3 },
  recommendation: { borderTopColor: colors.cream200, borderTopWidth: 1, paddingTop: 12, marginTop: 4 },
  recommendationText: { color: colors.cream700, fontSize: 14, marginTop: 8, lineHeight: 20 },
  topicRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, marginTop: 10 },
});
