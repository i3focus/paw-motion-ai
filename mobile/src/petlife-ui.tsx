import { useEffect, useMemo, useRef, useState, type ReactNode } from 'react';
import { Image, KeyboardAvoidingView, Linking, Modal, Platform, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import { useRouter } from 'expo-router';
import type { ReactionType } from '@petlife/design-tokens';
import { addComment, getComments, react, recordAdEvent, unreact, type Comment, type FeedCard } from './api/petlife';
import { colors, fonts, fontSize, radius, shadow } from './theme';
import { Badge, Button, Input, ReactionBar, SponsoredLabel } from './components';

export function Avatar({ name, imageUrl, size = 44 }: { name: string; imageUrl?: string | null; size?: number }) {
  const initials = name.split(' ').filter(Boolean).slice(0, 2).map((s) => s[0]).join('').toUpperCase() || 'PL';
  const style = { width: size, height: size, borderRadius: size / 2 };
  if (imageUrl) return <Image source={{ uri: imageUrl }} style={[style, s.avatarImg]} accessibilityLabel={name} />;
  return <View style={[style, s.avatar]} accessibilityLabel={name}><Text style={[s.avatarText, { fontSize: size * 0.34 }]}>{initials}</Text></View>;
}

export function timeAgo(iso?: string | null): string {
  if (!iso) return '';
  const m = Math.floor((Date.now() - new Date(iso).getTime()) / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  return h < 24 ? `${h}h ago` : `${Math.floor(h / 24)}d ago`;
}

export function Sheet({ visible, title, onClose, children }: { visible: boolean; title: string; onClose: () => void; children: ReactNode }) {
  return (
    <Modal visible={visible} transparent animationType="slide" onRequestClose={onClose}>
      <Pressable style={s.scrim} onPress={onClose} accessibilityLabel="Close" />
      <KeyboardAvoidingView behavior={Platform.OS === 'ios' ? 'padding' : undefined} style={s.sheetWrap}>
        <View style={s.sheet}>
          <View style={s.grabber} />
          <View style={s.sheetHead}>
            <Text style={s.sheetTitle} accessibilityRole="header">{title}</Text>
            <Pressable onPress={onClose} accessibilityRole="button" accessibilityLabel="Close sheet" style={s.sheetX}><Text style={s.sheetXText}>×</Text></Pressable>
          </View>
          <ScrollView keyboardShouldPersistTaps="handled">{children}</ScrollView>
        </View>
      </KeyboardAvoidingView>
    </Modal>
  );
}

function FeedHeader({ card }: { card: FeedCard }) {
  const router = useRouter();
  const a = card.author;
  if (!a) return null;
  const name = a.petName ?? a.ownerName ?? 'PetLife friend';
  const meta = [a.petSpecies && a.petName ? a.petSpecies : null, a.neighborhood ?? (a.ownerName !== name ? a.ownerName : null), timeAgo(card.publishedAt)].filter(Boolean).join(' · ');
  const inner = (
    <View style={s.headRow}>
      <Avatar name={name} imageUrl={a.petAvatarUrl} size={40} />
      <View style={s.headText}>
        <Text style={s.headName} numberOfLines={1}>{name}</Text>
        <Text style={s.headMeta} numberOfLines={1}>{meta}</Text>
      </View>
    </View>
  );
  return a.petId ? <Pressable onPress={() => router.push(`/pet/${a.petId}`)}>{inner}</Pressable> : inner;
}

function MiniRoute() {
  return (
    <View style={s.routeBox}>
      <View style={[s.routeDot, { left: 8, bottom: 12 }]} />
      <View style={[s.routeLine, { transform: [{ rotate: '-14deg' }] }]} />
      <View style={[s.routeDot, s.routeDotEnd, { right: 8, top: 10 }]} />
    </View>
  );
}

export function FeedItemCard({ card }: { card: FeedCard }) {
  const sponsored = card.type === 'SPONSORED';
  const [reactions, setReactions] = useState(card.reactions ?? null);
  const [comments, setComments] = useState<Comment[] | null>(null);
  const [commentCount, setCommentCount] = useState(card.commentCount ?? 0);
  const [draft, setDraft] = useState('');
  const [open, setOpen] = useState(false);
  const adLogged = useRef(false);

  useEffect(() => {
    if (sponsored && card.sponsored?.campaignId && !adLogged.current) {
      adLogged.current = true;
      recordAdEvent(card.sponsored.campaignId, 'IMPRESSION').catch(() => {});
    }
  }, [sponsored, card.sponsored?.campaignId]);

  useEffect(() => {
    setReactions(card.reactions ?? null);
    setCommentCount(card.commentCount ?? 0);
  }, [card.reactions, card.commentCount]);

  const counts = useMemo(() => ({
    PAW: reactions?.counts?.PAW ?? 0,
    LAUGH: reactions?.counts?.LAUGH ?? 0,
    HEART: reactions?.counts?.HEART ?? 0,
  }), [reactions]);

  async function onReact(type: ReactionType) {
    if (!card.id) return;
    try {
      setReactions(reactions?.viewerReaction === type ? await unreact(card.id) : await react(card.id, type));
    } catch { /* keep prior state */ }
  }

  async function toggleComments() {
    if (!open && comments === null && card.id) {
      const page = await getComments(card.id).catch(() => null);
      setComments(page?.items ?? []);
    }
    setOpen(!open);
  }

  const [commentBusy, setCommentBusy] = useState(false);
  async function submitComment() {
    const body = draft.trim();
    if (!body || !card.id || commentBusy) return;
    setCommentBusy(true);
    const created = await addComment(card.id, body).catch(() => null);
    if (created) {
      setComments((c) => [...(c ?? []), created]);
      setCommentCount((n) => n + 1);
      setDraft('');
    }
    setCommentBusy(false);
  }

  const sp = card.sponsored;
  const act = card.activity;
  const ach = card.achievement;
  const post = card.post;

  return (
    <View style={[s.card, sponsored && s.cardSponsored]}>
      {sponsored ? (
        <View style={s.sponsoredHead}>
          <SponsoredLabel />
          <Text style={s.sponsoredBy} numberOfLines={1}>{sp?.partnerName}{sp?.verified ? ' · verified' : ''}</Text>
        </View>
      ) : null}
      <View style={s.cardPad}><FeedHeader card={card} /></View>

      {post?.media && post.media.length > 0 ? (
        <ScrollView horizontal pagingEnabled showsHorizontalScrollIndicator={false}>
          {post.media.map((m, i) => <Image key={m.id ?? i} source={{ uri: m.url ?? undefined }} style={s.media} accessibilityLabel={post.caption ?? 'Post photo'} />)}
        </ScrollView>
      ) : null}
      {sp?.mediaUrl ? <Image source={{ uri: sp.mediaUrl }} style={s.media} accessibilityLabel={sp.title ?? 'Sponsored'} /> : null}

      <View style={s.cardPad}>
        {ach ? (
          <View style={s.achBox}>
            <Text style={s.achEmoji}>{ach.emoji ?? '🏅'}</Text>
            <View style={s.flex1}>
              <Text style={s.achName}>{ach.name ?? 'New badge earned!'}</Text>
              <Text style={s.achDesc}>{ach.description}</Text>
            </View>
          </View>
        ) : null}

        {act ? (
          <View style={s.actBox}>
            <View style={s.rowBetween}>
              <Badge label={act.type?.replace('_', ' ') ?? 'Activity'} tone="accent" />
              <Text style={s.streak}>🔥 {timeAgo(act.startedAt)}</Text>
            </View>
            <MiniRoute />
            <View style={s.actStats}>
              <Text style={s.actStat}>{Math.round((act.durationSeconds ?? 0) / 60)} min</Text>
              {act.distanceMeters ? <Text style={s.actStat}>{(act.distanceMeters / 1000).toFixed(1)} km</Text> : null}
            </View>
            {act.notes ? <Text style={s.actNotes}>{act.notes}</Text> : null}
          </View>
        ) : null}

        {sponsored ? (
          <View>
            <Text style={s.sponsoredTitle}>{sp?.title}</Text>
            <Text style={s.sponsoredDesc}>{sp?.description}</Text>
            {sp?.ctaLabel ? (
              <Pressable style={s.cta} onPress={() => {
                if (sp.campaignId) recordAdEvent(sp.campaignId, 'CLICK').catch(() => {});
                if (sp.ctaUrl && /^https?:\/\//i.test(sp.ctaUrl)) Linking.openURL(sp.ctaUrl).catch(() => {});
              }}><Text style={s.ctaText}>{sp.ctaLabel}</Text></Pressable>
            ) : null}
          </View>
        ) : null}

        {post?.caption ? <Text style={s.caption}>{post.caption}</Text> : null}
        {post?.placeName ? <Text style={s.place}>📍 {post.placeName}</Text> : null}

        {!sponsored ? (
          <View style={s.reactionsRow}>
            <ReactionBar counts={counts} selected={(reactions?.viewerReaction ?? null) as ReactionType | null} onReact={onReact} />
            <Pressable onPress={toggleComments} accessibilityRole="button" accessibilityLabel={`${commentCount} comments`} style={s.commentBtn}>
              <Text style={s.commentBtnText}>{commentCount > 0 ? `💬 ${commentCount}` : '💬'}</Text>
            </Pressable>
          </View>
        ) : null}

        {open ? (
          <View style={s.comments}>
            {(comments ?? []).map((c) => (
              <View key={c.id} style={s.commentRow}>
                <Avatar name={c.ownerName ?? '?'} imageUrl={c.ownerAvatarUrl} size={28} />
                <Text style={s.commentText}><Text style={s.commentName}>{c.ownerName ?? 'PetLife friend'}</Text> {c.body}</Text>
              </View>
            ))}
            {comments === null ? <Text style={s.muted}>Loading…</Text> : null}
            <View style={s.commentForm}>
              <Input value={draft} onChangeText={setDraft} placeholder="Write a comment…" style={s.commentInput} />
              <Button label="Send" variant="secondary" onPress={submitComment} disabled={!draft.trim()} />
            </View>
          </View>
        ) : null}
      </View>
    </View>
  );
}

const s = StyleSheet.create({
  flex1: { flex: 1 },
  avatar: { backgroundColor: colors.tealLight, alignItems: 'center', justifyContent: 'center' },
  avatarImg: { backgroundColor: colors.sandDeep },
  avatarText: { color: colors.teal, fontFamily: fonts.w900 },
  scrim: { flex: 1, backgroundColor: 'rgba(9, 63, 63, 0.45)' },
  sheetWrap: { justifyContent: 'flex-end' },
  sheet: { backgroundColor: colors.cream, borderTopLeftRadius: 28, borderTopRightRadius: 28, padding: 20, paddingBottom: 34, maxHeight: '88%' },
  grabber: { alignSelf: 'center', width: 48, height: 6, borderRadius: 3, backgroundColor: colors.sandDeep, marginBottom: 14 },
  sheetHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', marginBottom: 14 },
  sheetTitle: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900 },
  sheetX: { width: 40, height: 40, borderRadius: 20, backgroundColor: colors.white, alignItems: 'center', justifyContent: 'center' },
  sheetXText: { fontSize: 22, color: colors.teal, fontFamily: fonts.w900, marginTop: -2 },
  card: { backgroundColor: colors.white, borderRadius: radius.card, ...shadow.card, overflow: 'hidden', marginBottom: 14 },
  cardSponsored: { borderColor: colors.amber, borderWidth: 2 },
  cardPad: { paddingHorizontal: 16, paddingVertical: 12 },
  headRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  headText: { flex: 1, minWidth: 0 },
  headName: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900 },
  headMeta: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w700, marginTop: 1 },
  media: { width: 340, height: 260, backgroundColor: colors.sand },
  sponsoredHead: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderBottomColor: colors.line, borderBottomWidth: 1, paddingHorizontal: 16, paddingVertical: 10 },
  sponsoredBy: { color: colors.muted, fontSize: fontSize.label, fontFamily: fonts.w800, flexShrink: 1 },
  sponsoredTitle: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900, marginTop: 6 },
  sponsoredDesc: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 4 },
  cta: { alignSelf: 'flex-start', backgroundColor: colors.coral, borderRadius: radius.pill, paddingHorizontal: 18, paddingVertical: 10, marginTop: 10, ...shadow.coral },
  ctaText: { color: colors.coralInk, fontSize: fontSize.label, fontFamily: fonts.w900 },
  achBox: { flexDirection: 'row', alignItems: 'center', gap: 12, backgroundColor: colors.amberSoft, borderRadius: radius.md, padding: 14 },
  achEmoji: { fontSize: 30 },
  achName: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900 },
  achDesc: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 2 },
  actBox: { backgroundColor: colors.sand, borderRadius: radius.md, padding: 12, gap: 8 },
  rowBetween: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between' },
  streak: { color: colors.coralInk, fontSize: fontSize.caption, fontFamily: fonts.w900 },
  routeBox: { height: 64, borderRadius: radius.sm, backgroundColor: colors.tealLight, overflow: 'hidden' },
  routeDot: { position: 'absolute', width: 10, height: 10, borderRadius: 5, backgroundColor: colors.teal },
  routeDotEnd: { backgroundColor: colors.coral },
  routeLine: { position: 'absolute', left: 20, right: 20, top: 32, height: 3, borderRadius: 2, backgroundColor: colors.teal, opacity: 0.5 },
  actStats: { flexDirection: 'row', gap: 14 },
  actStat: { color: colors.teal, fontSize: fontSize.label, fontFamily: fonts.w800 },
  actNotes: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600 },
  caption: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 10 },
  place: { color: colors.muted, fontSize: fontSize.label, fontFamily: fonts.w800, marginTop: 6 },
  reactionsRow: { flexDirection: 'row', alignItems: 'center', justifyContent: 'space-between', borderTopColor: colors.line, borderTopWidth: 1, marginTop: 12, paddingTop: 10 },
  commentBtn: { minHeight: 44, justifyContent: 'center', paddingHorizontal: 8 },
  commentBtnText: { fontSize: fontSize.body },
  comments: { borderTopColor: colors.line, borderTopWidth: 1, marginTop: 8, paddingTop: 10, gap: 10 },
  commentRow: { flexDirection: 'row', gap: 8, alignItems: 'flex-start' },
  commentText: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, flex: 1 },
  commentName: { color: colors.ink, fontFamily: fonts.w900 },
  commentForm: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  commentInput: { flex: 1, marginBottom: 0 },
  muted: { color: colors.muted, fontSize: fontSize.label, fontFamily: fonts.w700 },
});
