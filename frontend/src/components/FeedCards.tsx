import { useEffect, useMemo, useRef, useState } from 'react';
import { Link } from 'react-router-dom';
import type { ReactionType } from '@petlife/design-tokens';
import { addComment, getComments, react, recordAdEvent, unreact, type Comment, type FeedCard } from '../api/petlife';
import { Avatar, Badge, ReactionBar, SponsoredLabel } from './ui';
import { PawIcon } from './petVisuals';

function timeAgo(iso?: string | null): string {
  if (!iso) return '';
  const diff = Date.now() - new Date(iso).getTime();
  const m = Math.floor(diff / 60000);
  if (m < 1) return 'just now';
  if (m < 60) return `${m}m ago`;
  const h = Math.floor(m / 60);
  if (h < 24) return `${h}h ago`;
  return `${Math.floor(h / 24)}d ago`;
}

export function FeedHeader({ card }: { card: FeedCard }) {
  const a = card.author;
  if (!a) return null;
  const name = a.petName ?? a.ownerName ?? 'PetLife friend';
  const meta = [a.petSpecies && a.petName ? a.petSpecies : null, a.neighborhood ?? a.ownerName !== name ? a.ownerName : null, timeAgo(card.publishedAt)].filter(Boolean).join(' · ');
  const inner = (
    <div className="flex items-center gap-3">
      <Avatar name={name} imageUrl={a.petAvatarUrl} size="sm" />
      <div className="min-w-0 flex-1">
        <p className="truncate font-extrabold text-ink">{name}</p>
        <p className="truncate text-caption text-muted">{meta}</p>
      </div>
    </div>
  );
  return a.petId ? <Link to={`/app/pets/${a.petId}`} className="block">{inner}</Link> : inner;
}

function MiniRoute() {
  return (
    <svg viewBox="0 0 200 80" aria-hidden="true" className="h-20 w-full rounded-lg bg-teal-light">
      <path d="M14 62 C 40 20, 70 74, 100 44 S 160 18, 186 40" fill="none" stroke="var(--color-teal)" strokeWidth="3" strokeDasharray="6 5" strokeLinecap="round" />
      <circle cx="14" cy="62" r="5" fill="var(--color-teal)" />
      <circle cx="186" cy="40" r="6" fill="var(--color-coral)" />
    </svg>
  );
}

export function FeedItemCard({ card }: { card: FeedCard }) {
  const sponsored = card.type === 'SPONSORED';
  const [reactions, setReactions] = useState(card.reactions ?? null);
  const [comments, setComments] = useState<Comment[] | null>(null);
  const [commentCount, setCommentCount] = useState(card.commentCount ?? 0);
  const [commentText, setCommentText] = useState('');
  const [showComments, setShowComments] = useState(false);
  const adLogged = useRef(false);

  useEffect(() => {
    if (sponsored && card.sponsored?.campaignId && !adLogged.current) {
      adLogged.current = true;
      recordAdEvent(card.sponsored.campaignId, 'IMPRESSION').catch(() => {});
    }
  }, [sponsored, card.sponsored?.campaignId]);

  const counts = useMemo(() => ({
    PAW: reactions?.counts?.PAW ?? 0,
    LAUGH: reactions?.counts?.LAUGH ?? 0,
    HEART: reactions?.counts?.HEART ?? 0,
  }), [reactions]);

  async function onReact(type: ReactionType) {
    const id = card.id;
    if (!id) return;
    try {
      if (reactions?.viewerReaction === type) setReactions(await unreact(id));
      else setReactions(await react(id, type));
    } catch { /* keep prior state */ }
  }

  async function toggleComments() {
    if (!showComments && comments === null && card.id) {
      const page = await getComments(card.id).catch(() => null);
      setComments(page?.items ?? []);
    }
    setShowComments(!showComments);
  }

  async function submitComment() {
    const body = commentText.trim();
    if (!body || !card.id) return;
    const created = await addComment(card.id, body).catch(() => null);
    if (created) {
      setComments((c) => [...(c ?? []), created]);
      setCommentCount((n) => n + 1);
      setCommentText('');
    }
  }

  const s = card.sponsored;
  const act = card.activity;
  const ach = card.achievement;
  const post = card.post;

  return (
    <article className={sponsored ? 'pl-card ring-2 ring-inset ring-amber' : 'pl-card'}>
      {sponsored ? (
        <div className="flex items-center justify-between gap-2 border-b border-line px-5 py-3">
          <SponsoredLabel />
          <p className="truncate text-label font-bold text-muted">{s?.partnerName}{s?.verified ? ' · verified' : ''}</p>
        </div>
      ) : null}

      <div className="px-5 pt-4"><FeedHeader card={card} /></div>

      {post?.media && post.media.length > 0 ? (
        <div className={`mt-3 grid gap-1 ${post.media.length > 1 ? 'grid-cols-2' : ''}`}>
          {post.media.map((m) => <img key={m.id} src={m.url ?? undefined} alt={post.caption ?? 'Post photo'} className="h-64 w-full object-cover" loading="lazy" />)}
        </div>
      ) : null}
      {s?.mediaUrl ? <img src={s.mediaUrl} alt={s.title ?? 'Sponsored'} className="mt-3 h-56 w-full object-cover" loading="lazy" /> : null}

      <div className="space-y-3 px-5 py-4">
        {ach ? (
          <div className="flex items-center gap-4 rounded-lg bg-amber-soft p-4">
            <span className="grid h-14 w-14 shrink-0 place-items-center rounded-full bg-amber text-2xl text-amber-ink" aria-hidden="true">{ach.emoji ?? '🏅'}</span>
            <div>
              <p className="font-extrabold text-ink">{ach.name ?? 'New badge earned!'}</p>
              <p className="text-body text-muted">{ach.description}</p>
            </div>
          </div>
        ) : null}

        {act ? (
          <div className="space-y-2 rounded-lg bg-sand p-4">
            <div className="flex items-center justify-between">
              <Badge tone="accent">{act.type?.replace('_', ' ') ?? 'Activity'}</Badge>
              <span className="text-caption font-extrabold text-coral-ink">🔥 {timeAgo(act.startedAt)}</span>
            </div>
            <MiniRoute />
            <div className="flex gap-4 text-label font-bold text-teal">
              <span>{Math.round((act.durationSeconds ?? 0) / 60)} min</span>
              {act.distanceMeters ? <span>{(act.distanceMeters / 1000).toFixed(1)} km</span> : null}
            </div>
            {act.notes ? <p className="text-body text-muted">{act.notes}</p> : null}
          </div>
        ) : null}

        {sponsored ? (
          <>
            <p className="text-title font-extrabold text-teal-dark">{s?.title}</p>
            <p className="text-body text-muted">{s?.description}</p>
            {s?.ctaLabel ? (
              <a href={s.ctaUrl ?? '#'} onClick={() => s?.campaignId && recordAdEvent(s.campaignId, 'CLICK').catch(() => {})}
                className="inline-flex min-h-11 items-center rounded-pill bg-coral px-5 text-label font-black text-coral-ink shadow-coral">
                {s.ctaLabel}
              </a>
            ) : null}
          </>
        ) : null}

        {post?.caption ? <p className="text-body text-ink">{post.caption}</p> : null}
        {post?.placeName ? <p className="text-label font-bold text-muted">📍 {post.placeName}</p> : null}

        {!sponsored ? (
          <div className="flex flex-wrap items-center justify-between gap-2 border-t border-line pt-3">
            <ReactionBar counts={counts} selected={(reactions?.viewerReaction ?? null) as ReactionType | null} onReact={onReact} />
            <button type="button" onClick={toggleComments} className="min-h-11 rounded-pill px-3 text-label font-extrabold text-teal hover:bg-teal-light">
              {commentCount > 0 ? `${commentCount} comments` : 'Comment'}
            </button>
          </div>
        ) : null}

        {showComments ? (
          <div className="space-y-3 border-t border-line pt-3">
            {(comments ?? []).map((c) => (
              <div key={c.id} className="flex gap-2 text-body">
                <Avatar name={c.ownerName ?? '?'} imageUrl={c.ownerAvatarUrl} size="sm" className="h-8 w-8" />
                <p className="min-w-0"><span className="font-extrabold text-ink">{c.ownerName ?? 'PetLife friend'}</span> <span className="text-muted">{c.body}</span></p>
              </div>
            ))}
            {comments === null ? <p className="text-label text-muted">Loading…</p> : null}
            <form className="flex gap-2" onSubmit={(e) => { e.preventDefault(); void submitComment(); }}>
              <input value={commentText} onChange={(e) => setCommentText(e.target.value)} placeholder="Write a comment…" aria-label="Write a comment"
                className="min-h-11 flex-1 rounded-pill border-2 border-line bg-white px-4 text-body text-ink outline-none focus:border-teal" />
              <button type="submit" disabled={!commentText.trim()} className="min-h-11 rounded-pill bg-teal px-4 text-label font-extrabold text-white disabled:opacity-50">Send</button>
            </form>
          </div>
        ) : null}
      </div>
    </article>
  );
}

export function EmptyFeed() {
  return (
    <div className="pl-card-sand flex flex-col items-center p-10 text-center">
      <PawIcon className="h-12 w-12 text-teal" />
      <h2 className="mt-4 text-title">The park is quiet… for now</h2>
      <p className="mt-2 max-w-sm text-body text-muted">Follow other pets in your neighborhood, share a moment, and your feed will fill up with tails, trails and trophies.</p>
    </div>
  );
}
