import { useEffect, useRef, useState } from 'react';
import { FlatList, KeyboardAvoidingView, Platform, StyleSheet, Text, View } from 'react-native';
import { cancelAction, confirmAction, createConversation, getAiUsage, getConversations, getMessages, sendMessage, type AiAction, type AiMessage, type AiUsage, type Conversation } from '../../src/api/petlife';
import { Badge, Button, Chip, ErrorText, Input, LoadingState, SponsoredLabel } from '../../src/components';
import { colors, fonts, fontSize, radius, shadow } from '../../src/theme';

const QUICK = ['What can we do this weekend?', 'Pet-friendly places nearby', 'Write a caption', 'Week summary'];
const ACTION_EMOJI: Record<string, string> = { BOOKING: '📅', REMINDER: '🔔', CAPTION: '✍️', PLAYDATE: '🐶' };

function ActionCard({ action, onUpdate }: { action: AiAction; onUpdate: (a: AiAction) => void }) {
  const [busy, setBusy] = useState(false);
  const p = (action.payload ?? {}) as Record<string, unknown>;
  const title = String(p.title ?? p.serviceName ?? p.label ?? 'Suggested action');
  const meta = [p.distanceMeters ? `${(Number(p.distanceMeters) / 1000).toFixed(1)} km` : null, p.when ?? p.startsAt ? new Date(String(p.when ?? p.startsAt)).toLocaleString() : null].filter(Boolean).join(' · ');
  function act(fn: () => Promise<AiAction>) {
    setBusy(true);
    fn().then(onUpdate).catch(() => {}).finally(() => setBusy(false));
  }
  return (
    <View style={s.actionCard}>
      <View style={s.actionRow}>
        <Text style={s.actionEmoji}>{ACTION_EMOJI[action.type ?? ''] ?? '✨'}</Text>
        <View style={s.flex1}>
          <Text style={s.actionTitle}>{title}</Text>
          {meta ? <Text style={s.actionMeta}>{meta}</Text> : null}
          {action.sponsored ? <SponsoredLabel /> : null}
        </View>
      </View>
      {action.status === 'PROPOSED' ? (
        <View style={s.actionBtns}>
          <View style={s.flex1}><Button label={action.type === 'BOOKING' ? 'Book' : 'Confirm'} disabled={busy} onPress={() => act(() => confirmAction(action.id!))} /></View>
          <View style={s.flex1}><Button label="Cancel" variant="ghost" disabled={busy} onPress={() => act(() => cancelAction(action.id!))} /></View>
        </View>
      ) : (
        <Badge tone={action.status === 'EXECUTED' || action.status === 'CONFIRMED' ? 'success' : action.status === 'FAILED' ? 'danger' : 'neutral'}
          label={action.status === 'EXECUTED' ? '✓ Done' : action.status === 'CONFIRMED' ? 'Confirmed' : action.status === 'FAILED' ? 'Failed' : 'Cancelled'} />
      )}
    </View>
  );
}

export default function AiScreen() {
  const [active, setActive] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<AiMessage[]>([]);
  const [usage, setUsage] = useState<AiUsage | null>(null);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const listRef = useRef<FlatList<AiMessage>>(null);

  useEffect(() => {
    getAiUsage().then(setUsage).catch(() => setUsage(null));
    getConversations()
      .then(async (page) => {
        const convo = (page.items ?? [])[0] ?? (await createConversation().catch(() => null));
        setActive(convo);
        if (convo?.id) setMessages(await getMessages(convo.id).catch(() => []));
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not start PetLife AI'))
      .finally(() => setLoading(false));
  }, []);

  async function send(content: string) {
    const text = content.trim();
    if (!text || sending) return;
    let convo = active;
    if (!convo) {
      convo = await createConversation().catch(() => null);
      if (!convo) { setError('PetLife AI is unavailable right now'); return; }
      setActive(convo);
    }
    setSending(true);
    setInput('');
    setError(null);
    setMessages((m) => [...m, { id: `tmp-${Date.now()}`, role: 'USER', content: text, createdAt: new Date().toISOString() }]);
    try {
      const res = await sendMessage(convo.id!, text);
      if (res.assistantMessage) setMessages((m) => [...m, res.assistantMessage!]);
      if (res.usage) setUsage(res.usage);
    } catch (e) {
      setMessages((m) => [...m, { role: 'ASSISTANT', content: 'I hit a snag. Try again in a moment.' }]);
      setError(e instanceof Error ? e.message : 'Message failed');
    } finally {
      setSending(false);
    }
  }

  if (loading) return <LoadingState label="Waking up PetLife AI…" />;

  return (
    <KeyboardAvoidingView style={s.screen} behavior={Platform.OS === 'ios' ? 'padding' : undefined} keyboardVerticalOffset={90}>
      <View style={s.head}>
        <Text style={s.paw} accessibilityElementsHidden>🐾</Text>
        <View style={s.flex1}>
          <Text style={s.title} accessibilityRole="header">PetLife AI</Text>
          <Text style={s.sub}>Your assistant for your pet's world</Text>
        </View>
        {usage ? <Badge tone="accent" label={usage.plan === 'FREE' ? `${usage.used}/${usage.limit ?? '∞'}` : 'Unlimited'} /> : null}
      </View>

      <FlatList
        ref={listRef}
        data={messages}
        keyExtractor={(m, i) => m.id ?? String(i)}
        contentContainerStyle={s.chat}
        onContentSizeChange={() => listRef.current?.scrollToEnd({ animated: true })}
        ListEmptyComponent={
          <View style={s.intro}>
            <Text style={s.introTitle}>Hi! I'm PetLife AI 🐾</Text>
            <Text style={s.introText}>I can plan weekends, find pet-friendly spots, write captions, schedule reminders and book partner services — always with your confirmation.</Text>
          </View>
        }
        renderItem={({ item: m }) => (
          <View>
            <View style={[s.bubble, m.role === 'USER' ? s.bubbleUser : s.bubbleAi]}>
              <Text style={[s.bubbleText, m.role === 'USER' && s.bubbleTextUser]}>{m.content}</Text>
            </View>
            {m.action ? <ActionCard action={m.action} onUpdate={(a) => setMessages((list) => list.map((x) => (x.action?.id === a.id ? { ...x, action: a } : x)))} /> : null}
          </View>
        )}
        ListFooterComponent={sending ? <View style={[s.bubble, s.bubbleAi]}><Text style={s.bubbleText}>Thinking…</Text></View> : undefined}
      />

      <View style={s.quickRow}>
        {QUICK.map((q) => <Chip key={q} label={q} onPress={() => void send(q)} />)}
      </View>
      <ErrorText message={error} />
      <View style={s.form}>
        <Input value={input} onChangeText={setInput} placeholder="Ask PetLife AI…" style={s.formInput} onSubmitEditing={() => void send(input)} />
        <Button label="Send" onPress={() => void send(input)} disabled={sending || !input.trim()} />
      </View>
      <Text style={s.disclaimer}>PetLife AI only acts after your confirmation. It is not a veterinarian.</Text>
    </KeyboardAvoidingView>
  );
}

const s = StyleSheet.create({
  screen: { flex: 1, backgroundColor: colors.cream, padding: 16 },
  flex1: { flex: 1 },
  head: { flexDirection: 'row', alignItems: 'center', gap: 10, borderBottomColor: colors.line, borderBottomWidth: 1, paddingBottom: 12 },
  paw: { fontSize: 30, width: 48, height: 48, textAlign: 'center', textAlignVertical: 'center', backgroundColor: colors.teal, borderRadius: 24, color: colors.white, overflow: 'hidden' },
  title: { color: colors.tealDark, fontSize: fontSize.title, fontFamily: fonts.w900 },
  sub: { color: colors.muted, fontSize: fontSize.label, fontFamily: fonts.w700 },
  chat: { paddingVertical: 14, gap: 12 },
  intro: { backgroundColor: colors.sand, borderRadius: radius.card, padding: 18, alignItems: 'center' },
  introTitle: { color: colors.tealDark, fontSize: fontSize.bodyLg, fontFamily: fonts.w900 },
  introText: { color: colors.muted, fontSize: fontSize.body, fontFamily: fonts.w600, marginTop: 6, textAlign: 'center' },
  bubble: { maxWidth: '85%', borderRadius: 18, paddingHorizontal: 14, paddingVertical: 10 },
  bubbleUser: { alignSelf: 'flex-end', backgroundColor: colors.teal, borderTopRightRadius: 4 },
  bubbleAi: { alignSelf: 'flex-start', backgroundColor: colors.sand, borderTopLeftRadius: 4 },
  bubbleText: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w600 },
  bubbleTextUser: { color: colors.white },
  actionCard: { alignSelf: 'flex-start', maxWidth: '90%', backgroundColor: colors.white, borderColor: colors.line, borderWidth: 1, borderRadius: radius.md, padding: 12, marginTop: 6, ...shadow.card },
  actionRow: { flexDirection: 'row', alignItems: 'center', gap: 10 },
  actionEmoji: { fontSize: 24 },
  actionTitle: { color: colors.ink, fontSize: fontSize.body, fontFamily: fonts.w900 },
  actionMeta: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w700, marginTop: 2 },
  actionBtns: { flexDirection: 'row', gap: 8, marginTop: 10 },
  quickRow: { flexDirection: 'row', flexWrap: 'wrap', gap: 8, paddingVertical: 8 },
  form: { flexDirection: 'row', gap: 8, alignItems: 'center' },
  formInput: { flex: 1, marginBottom: 0 },
  disclaimer: { color: colors.muted, fontSize: fontSize.caption, fontFamily: fonts.w600, marginTop: 8 },
});
