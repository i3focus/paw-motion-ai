import { useCallback, useEffect, useRef, useState } from 'react';
import { FlatList, Image, Modal, Pressable, RefreshControl, StyleSheet, Text, View } from 'react-native';
import { useFocusEffect } from 'expo-router';
import { getFeed, getStories, viewStory, type FeedCard, type StoryGroup } from '../../src/api/petlife';
import { Avatar, FeedItemCard } from '../../src/petlife-ui';
import { EmptyState, LoadingState } from '../../src/components';
import { colors, fonts, fontSize, radius, shadow } from '../../src/theme';

function StoryRail({ groups, onOpen }: { groups: StoryGroup[]; onOpen: (g: StoryGroup, i: number) => void }) {
  if (groups.length === 0) return null;
  return (
    <View style={st.rail}>
      {groups.map((g, i) => {
        const fresh = !g.own && !(g.allViewed ?? true);
        return (
          <Pressable key={g.petId ?? g.ownerId} onPress={() => onOpen(g, i)} accessibilityRole="button" accessibilityLabel={`${g.petName}'s story`} style={st.storyCol}>
            <View style={[st.ring, fresh ? st.ringNew : st.ringSeen, fresh && st.ringGlow]}>
              <View style={st.ringInner}><Avatar name={g.petName ?? '?'} imageUrl={g.petAvatarUrl} size={56} /></View>
              {g.own ? <View style={st.addBadge}><Text style={st.addBadgeText}>+</Text></View> : null}
            </View>
            <Text style={st.storyName} numberOfLines={1}>{g.own ? 'You' : g.petName}</Text>
          </Pressable>
        );
      })}
    </View>
  );
}

function StoryViewer({ group, index, onClose }: { group: StoryGroup | null; index: number; onClose: () => void }) {
  const [i, setI] = useState(index);
  const stories = group?.stories ?? [];
  const story = stories[i];
  useEffect(() => { setI(index); }, [index, group]);
  useEffect(() => { if (story?.id) viewStory(story.id).catch(() => {}); }, [story?.id]);
  if (!group || !story) return null;
  return (
    <Modal visible transparent animationType="fade" onRequestClose={onClose}>
      <View style={st.viewer}>
        <View style={st.viewerHead}>
          <Avatar name={group.petName ?? '?'} imageUrl={group.petAvatarUrl} size={36} />
          <Text style={st.viewerName}>{group.petName}</Text>
          <Pressable onPress={onClose} accessibilityLabel="Close story" style={st.viewerX}><Text style={st.viewerXText}>×</Text></Pressable>
        </View>
        {story.mediaUrl ? <Image source={{ uri: story.mediaUrl }} style={st.viewerImg} resizeMode="cover" /> : <View style={st.viewerImg}><Text style={st.viewerPaw}>🐾</Text></View>}
        {story.caption ? <Text style={st.viewerCap}>{story.caption}</Text> : null}
        <View style={st.viewerNav}>
          <Pressable disabled={i === 0} onPress={() => setI(i - 1)} style={[st.viewerBtn, i === 0 && st.dim]}><Text style={st.viewerBtnText}>Back</Text></Pressable>
          <Text style={st.viewerCount}>{i + 1}/{stories.length}</Text>
          <Pressable onPress={() => (i + 1 < stories.length ? setI(i + 1) : onClose())} style={st.viewerBtn}><Text style={st.viewerBtnText}>{i + 1 < stories.length ? 'Next' : 'Done'}</Text></Pressable>
        </View>
      </View>
    </Modal>
  );
}

export default function FeedScreen() {
  const [cards, setCards] = useState<FeedCard[] | null>(null);
  const [stories, setStories] = useState<StoryGroup[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [refreshing, setRefreshing] = useState(false);
  const [viewer, setViewer] = useState<{ group: StoryGroup; index: number } | null>(null);
  const loadingMore = useRef(false);

  const load = useCallback((target: number, append: boolean) => {
    if (append && loadingMore.current) return Promise.resolve();
    loadingMore.current = true;
    return getFeed(target, 20).then((res) => {
      setCards((prev) => (append ? [...(prev ?? []), ...(res.items ?? [])] : res.items ?? []));
      setPage(target);
      setHasNext(Boolean(res.hasNext));
    }).catch(() => setCards((c) => c ?? []))
      .finally(() => { loadingMore.current = false; });
  }, []);

  useFocusEffect(useCallback(() => {
    load(0, false);
    getStories().then(setStories).catch(() => setStories([]));
  }, [load]));

  if (cards === null) return <LoadingState label="Loading the feed…" />;

  return (
    <View style={st.screen}>
      <FlatList
        data={cards}
        keyExtractor={(c, i) => c.id ?? `feed-${i}`}
        renderItem={({ item }) => <FeedItemCard card={item} />}
        ListHeaderComponent={<StoryRail groups={stories} onOpen={(group, index) => setViewer({ group, index })} />}
        ListEmptyComponent={<EmptyState title="The park is quiet… for now" description="Follow other pets nearby and share a moment — your feed will fill up with tails, trails and trophies." />}
        contentContainerStyle={st.list}
        refreshControl={<RefreshControl refreshing={refreshing} tintColor={colors.teal} onRefresh={() => { setRefreshing(true); load(0, false).finally(() => setRefreshing(false)); getStories().then(setStories).catch(() => {}); }} />}
        onEndReached={() => { if (hasNext) load(page + 1, true); }}
        onEndReachedThreshold={0.5}
      />
      <StoryViewer group={viewer?.group ?? null} index={viewer?.index ?? 0} onClose={() => setViewer(null)} />
    </View>
  );
}

const st = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream },
  list: { padding: 16, paddingBottom: 30 },
  rail: { flexDirection: 'row', gap: 14, marginBottom: 14 },
  storyCol: { alignItems: 'center', width: 68 },
  ring: { width: 66, height: 66, borderRadius: 33, padding: 3, alignItems: 'center', justifyContent: 'center' },
  ringNew: { backgroundColor: colors.mint },
  ringSeen: { backgroundColor: colors.sandDeep },
  ringGlow: { shadowColor: colors.mint, shadowRadius: 10, shadowOpacity: 0.9, shadowOffset: { width: 0, height: 0 }, elevation: 6 },
  ringInner: { borderRadius: 30, borderWidth: 3, borderColor: colors.cream, overflow: 'hidden' },
  addBadge: { position: 'absolute', right: -1, bottom: -1, width: 24, height: 24, borderRadius: 12, backgroundColor: colors.teal, borderWidth: 3, borderColor: colors.cream, alignItems: 'center', justifyContent: 'center' },
  addBadgeText: { color: colors.white, fontSize: 14, fontFamily: fonts.w900, marginTop: -2 },
  storyName: { color: colors.ink, fontSize: fontSize.caption, fontFamily: fonts.w800, marginTop: 5, maxWidth: 68 },
  viewer: { flex: 1, backgroundColor: 'rgba(9,63,63,0.95)', justifyContent: 'center' },
  viewerHead: { position: 'absolute', top: 56, left: 16, right: 16, flexDirection: 'row', alignItems: 'center', gap: 10, zIndex: 1 },
  viewerName: { color: colors.white, fontSize: fontSize.body, fontFamily: fonts.w900, flex: 1 },
  viewerX: { width: 36, height: 36, borderRadius: 18, backgroundColor: 'rgba(255,255,255,0.2)', alignItems: 'center', justifyContent: 'center' },
  viewerXText: { color: colors.white, fontSize: 20, marginTop: -2 },
  viewerImg: { width: '100%', height: '62%', backgroundColor: colors.tealDark, alignItems: 'center', justifyContent: 'center' },
  viewerPaw: { fontSize: 60 },
  viewerCap: { color: colors.white, fontSize: fontSize.body, fontFamily: fonts.w600, padding: 16 },
  viewerNav: { position: 'absolute', bottom: 40, left: 16, right: 16, flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  viewerBtn: { backgroundColor: 'rgba(255,255,255,0.2)', borderRadius: radius.pill, paddingHorizontal: 18, paddingVertical: 10 },
  viewerBtnText: { color: colors.white, fontFamily: fonts.w900, fontSize: fontSize.label },
  viewerCount: { color: 'rgba(255,255,255,0.7)', fontFamily: fonts.w800 },
  dim: { opacity: 0.4 },
});
