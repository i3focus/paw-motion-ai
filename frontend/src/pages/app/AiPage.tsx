import { useEffect, useRef, useState } from 'react';
import { confirmAction, cancelAction, createConversation, getAiUsage, getConversations, getMessages, sendMessage, type AiAction, type AiMessage, type AiUsage, type Conversation } from '../../api/petlife';
import { Badge, Button, ErrorMessage, LoadingState, SponsoredLabel } from '../../components/ui';
import { PawIcon } from '../../components/petVisuals';

const QUICK = ['What can we do this weekend?', 'Pet-friendly places nearby', 'Write a caption', 'Week summary'];

function ActionCard({ action, onUpdate }: { action: AiAction; onUpdate: (a: AiAction) => void }) {
  const [busy, setBusy] = useState(false);
  const p = (action.payload ?? {}) as Record<string, unknown>;
  const title = String(p.title ?? p.serviceName ?? p.label ?? 'Suggested action');
  const meta = [p.distanceMeters ? `${(Number(p.distanceMeters) / 1000).toFixed(1)} km` : null, p.when ?? p.startsAt ? new Date(String(p.when ?? p.startsAt)).toLocaleString() : null].filter(Boolean).join(' · ');
  async function act(fn: () => Promise<AiAction>) {
    setBusy(true);
    try { onUpdate(await fn()); } finally { setBusy(false); }
  }
  return (
    <div className="rounded-xl border border-line bg-white p-3">
      <div className="flex items-center gap-3">
        <span className="text-2xl" aria-hidden="true">{action.type === 'BOOKING' ? '📅' : action.type === 'REMINDER' ? '🔔' : action.type === 'PLAYDATE' ? '🐶' : '✍️'}</span>
        <div className="min-w-0 flex-1">
          <p className="font-extrabold text-ink">{title}</p>
          {meta ? <p className="text-caption text-muted">{meta}</p> : null}
          {action.sponsored ? <SponsoredLabel /> : null}
        </div>
      </div>
      {action.status === 'PROPOSED' ? (
        <div className="mt-2 flex gap-2">
          <Button size="sm" disabled={busy} onClick={() => void act(() => confirmAction(action.id!))}>{action.type === 'BOOKING' ? 'Book' : 'Confirm'}</Button>
          <Button size="sm" variant="ghost" disabled={busy} onClick={() => void act(() => cancelAction(action.id!))}>Cancel</Button>
        </div>
      ) : (
        <Badge tone={action.status === 'EXECUTED' || action.status === 'CONFIRMED' ? 'success' : action.status === 'FAILED' ? 'danger' : 'neutral'}>
          {action.status === 'EXECUTED' ? '✓ Done' : action.status === 'CONFIRMED' ? 'Confirmed' : action.status === 'FAILED' ? `Failed${action.error ? `: ${action.error}` : ''}` : 'Cancelled'}
        </Badge>
      )}
    </div>
  );
}

export function AiPage() {
  const [active, setActive] = useState<Conversation | null>(null);
  const [messages, setMessages] = useState<AiMessage[]>([]);
  const [usage, setUsage] = useState<AiUsage | null>(null);
  const [input, setInput] = useState('');
  const [sending, setSending] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const bottomRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    getAiUsage().then(setUsage).catch(() => setUsage(null));
    getConversations()
      .then(async (page) => {
        const existing = (page.items ?? [])[0] ?? null;
        const convo = existing ?? (await createConversation().catch(() => null));
        setActive(convo);
        if (convo?.id) setMessages(await getMessages(convo.id).catch(() => []));
      })
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not start PetLife AI'))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => { bottomRef.current?.scrollIntoView({ behavior: 'smooth' }); }, [messages.length]);

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
    setMessages((m) => [...m, { role: 'USER', content: text, createdAt: new Date().toISOString() }]);
    try {
      const res = await sendMessage(convo.id!, text);
      if (res.assistantMessage) setMessages((m) => [...m, res.assistantMessage!]);
      if (res.usage) setUsage(res.usage);
    } catch (e) {
      setMessages((m) => [...m, { role: 'ASSISTANT', content: 'I hit a snag. Try again in a moment — or check your plan limit below.' }]);
      setError(e instanceof Error ? e.message : 'Message failed');
    } finally {
      setSending(false);
    }
  }

  function updateAction(updated: AiAction) {
    setMessages((list) => list.map((m) => m.action?.id === updated.id ? { ...m, action: updated } : m));
  }

  if (loading) return <LoadingState label="Waking up PetLife AI…" />;

  return (
    <div className="mx-auto flex max-w-2xl flex-col" style={{ minHeight: 'calc(100vh - 220px)' }}>
      <div className="flex items-center gap-3 border-b border-line pb-4">
        <span className="grid h-12 w-12 place-items-center rounded-full bg-teal text-white"><PawIcon className="h-6 w-6" /></span>
        <div className="flex-1">
          <h1 className="text-title">PetLife AI</h1>
          <p className="text-label text-muted">Your assistant for your pet's world</p>
        </div>
        {usage ? <Badge tone="accent">{usage.plan === 'FREE' ? `${usage.used}/${usage.limit ?? '∞'} messages` : 'Unlimited'}</Badge> : null}
      </div>

      <div className="flex-1 space-y-3 overflow-y-auto py-4" role="log" aria-label="Conversation" aria-live="polite">
        {messages.length === 0 ? (
          <div className="pl-card-sand p-5 text-center">
            <p className="font-extrabold text-teal-dark">Hi! I'm PetLife AI 🐾</p>
            <p className="mt-1 text-body text-muted">I can plan weekends, find pet-friendly spots, write captions, schedule reminders and book partner services — always with your confirmation.</p>
          </div>
        ) : messages.map((m, i) => (
          <div key={m.id ?? i} className="space-y-2">
            <div className={`w-fit max-w-[85%] rounded-2xl px-4 py-3 text-body ${m.role === 'USER' ? 'ml-auto rounded-tr-sm bg-teal font-semibold text-white' : 'rounded-tl-sm bg-sand text-ink'}`}>
              {m.content}
            </div>
            {m.action ? <div className="max-w-[85%]"><ActionCard action={m.action} onUpdate={updateAction} /></div> : null}
          </div>
        ))}
        {sending ? <div className="w-fit rounded-2xl rounded-tl-sm bg-sand px-4 py-3 text-body text-muted">Thinking…</div> : null}
        <div ref={bottomRef} />
      </div>

      <div className="flex flex-wrap gap-2 pb-3">
        {QUICK.map((q) => (
          <button key={q} type="button" onClick={() => void send(q)} disabled={sending}
            className="min-h-9 rounded-pill bg-white px-3 text-caption font-extrabold text-teal shadow-card hover:bg-teal-light disabled:opacity-50">{q}</button>
        ))}
      </div>
      <ErrorMessage message={error} />
      <form className="mt-2 flex gap-2" onSubmit={(e) => { e.preventDefault(); void send(input); }}>
        <input value={input} onChange={(e) => setInput(e.target.value)} placeholder="Ask PetLife AI…" aria-label="Ask PetLife AI"
          className="min-h-12 flex-1 rounded-pill border-2 border-line bg-white px-5 text-body font-semibold text-ink outline-none focus:border-teal" />
        <Button type="submit" disabled={sending || !input.trim()}>Send</Button>
      </form>
      <p className="mt-2 text-caption text-muted">PetLife AI only acts after your confirmation. It is not a veterinarian — for health concerns, see a professional.</p>
    </div>
  );
}
