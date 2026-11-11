import { useEffect, useState } from 'react';
import { Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import { getNotifications, markAllRead, markNotificationRead, type Notification } from '../src/api/petlife';
import { Badge, Button, EmptyState, ErrorText, LoadingState } from '../src/components';
import { colors, fonts, fontSize, radius, shadow } from '../src/theme';

const ICONS: Record<string, string> = {
  HEALTH_REMINDER: '🩺', ACHIEVEMENT: '🏅', CHALLENGE: '🎯', SOCIAL: '💬',
  PLAYDATE: '🐶', LOST_PET: '⚠️', BOOKING: '📅', SYSTEM: 'ℹ️',
};

const UUID_RE = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;

function routeFor(n: Notification): '/(tabs)/map' | '/discover' | `/pet/${string}` | null {
  const d = (n.data ?? {}) as Record<string, unknown>;
  if (typeof d.petId === 'string' && UUID_RE.test(d.petId)) return `/pet/${d.petId}`;
  if (n.type === 'LOST_PET' || n.type === 'PLAYDATE') return '/(tabs)/map';
  if (n.type === 'BOOKING' || n.type === 'ACHIEVEMENT' || n.type === 'CHALLENGE') return '/discover';
  return null;
}

export default function NotificationsScreen() {
  const router = useRouter();
  const [items, setItems] = useState<Notification[] | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getNotifications().then((r) => setItems(r.items ?? [])).catch((e) => setError(e instanceof Error ? e.message : 'Could not load notifications'));
  }, []);

  async function read(n: Notification) {
    const to = routeFor(n);
    if (!n.read && n.id) {
      await markNotificationRead(n.id).catch(() => {});
      setItems((l) => l?.map((i) => (i.id === n.id ? { ...i, read: true } : i)) ?? l);
    }
    if (to) router.push(to);
  }

  if (items === null && !error) return <LoadingState label="Loading notifications…" />;

  return (
    <ScrollView style={s.screen} contentContainerStyle={s.content}>
      <View style={s.head}>
        <Text style={s.h1} accessibilityRole="header">Notifications</Text>
        {(items ?? []).some((n) => !n.read) ? (
          <Button label="Mark all read" variant="ghost" onPress={() => markAllRead().then(() => setItems((l) => l?.map((i) => ({ ...i, read: true })) ?? l)).catch(() => {})} />
        ) : null}
      </View>
      <ErrorText message={error} />
      {(items ?? []).length === 0 && !error ? <EmptyState title="All caught up" description="Reminders, achievements and social updates show up here." /> : null}
      {(items ?? []).map((n) => (
        <Pressable key={n.id} style={[s.card, n.read && s.read]} onPress={() => void read(n)} accessibilityRole="button">
          <Text style={s.icon} accessibilityElementsHidden>{ICONS[n.type ?? 'SYSTEM']}</Text>
          <View style={s.flex1}>
            <View style={s.titleRow}>
              <Text style={s.title} numberOfLines={1}>{n.title}</Text>
              {!n.read ? <Badge label="New" tone="accent" /> : null}
            </View>
            <Text style={s.body}>{n.body}</Text>
            <Text style={s.meta}>{n.createdAt ? new Date(n.createdAt).toLocaleString() : ''}</Text>
          </View>
        </Pressable>
      ))}
    </ScrollView>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  content: { padding: 16, paddingBottom: 40 },
  head: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 },
  h1: { color: colors.tealDark, fontSize: fontSize.display, fontFamily: fonts.w900 },
  flex1: { flex: 1, minWidth: 0 },
  card: { flexDirection: 'row', gap: 12, backgroundColor: colors.white, borderRadius: radius.card, padding: 14, marginBottom: 10, ...shadow.card },
  read: { opacity: 0.65 },
  icon: { fontSize: 24 },
  titleRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  title: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900, flexShrink: 1 },
  body: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 3 },
  meta: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w600, marginTop: 4 },
});
