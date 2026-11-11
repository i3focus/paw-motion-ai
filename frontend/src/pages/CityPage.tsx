import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  addActivityComment,
  ApiError,
  listActivityComments,
  listActivityFeed,
  listMapPoints,
  toggleActivityReaction,
} from '../api/client';
import { Avatar, Badge, Button, Card, EmptyState, ErrorMessage, PoppyMark, SectionHeader, TextInput } from '../components/ui';
import type { ActivityComment, ActivityPost, MapPoints } from '../types';

const REACTIONS = [
  { type: 'LIKE', label: 'Like', icon: '♡' },
  { type: 'LOVE', label: 'Love', icon: '♥' },
  { type: 'CHEER', label: 'Cheer', icon: '★' },
] as const;

const FEED_PAGE_SIZE = 20;

function timeAgo(value: string): string {
  const seconds = Math.max(1, Math.floor((Date.now() - new Date(value).getTime()) / 1000));
  if (seconds < 60) return `${seconds}s ago`;
  const minutes = Math.floor(seconds / 60);
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  return `${Math.floor(hours / 24)}d ago`;
}

function duration(seconds: number): string {
  const minutes = Math.floor(seconds / 60);
  const rest = seconds % 60;
  return minutes ? `${minutes}m ${rest}s` : `${rest}s`;
}

function ActivityPostCard({ post, onUpdated }: { post: ActivityPost; onUpdated: (post: ActivityPost) => void }) {
  const [comments, setComments] = useState<ActivityComment[] | null>(null);
  const [commentsOpen, setCommentsOpen] = useState(false);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);

  async function loadComments() {
    setBusy(true);
    setError(null);
    try {
      setComments(await listActivityComments(post.id));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load comments');
    } finally {
      setBusy(false);
    }
  }

  async function toggleComments() {
    const next = !commentsOpen;
    setCommentsOpen(next);
    if (next && comments === null) {
      await loadComments();
    }
  }

  async function react(type: string) {
    setBusy(true);
    setError(null);
    try {
      onUpdated(await toggleActivityReaction(post.id, type));
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to react');
    } finally {
      setBusy(false);
    }
  }

  async function submitComment(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const body = comment.trim();
    if (!body) return;
    setBusy(true);
    setError(null);
    try {
      const created = await addActivityComment(post.id, body);
      setComments((current) => [...(current ?? []), created]);
      setComment('');
      onUpdated({ ...post, commentCount: post.commentCount + 1 });
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to comment');
    } finally {
      setBusy(false);
    }
  }

  return (
    <article className="overflow-hidden rounded-[28px] border border-cream-200 bg-white shadow-[0_24px_60px_-35px_rgba(54,39,20,0.4)]">
      <header className="flex items-center gap-3 border-b border-cream-100 px-4 py-3">
        <Avatar name={post.ownerName} src={post.ownerAvatarUrl} className="h-11 w-11" />
        <div className="min-w-0 flex-1">
          <p className="truncate font-semibold text-cream-900">{post.ownerName}</p>
          <p className="truncate text-xs text-cream-500">{post.petName} · {post.petSpecies} · {timeAgo(post.startedAt)}</p>
        </div>
        <Badge tone="accent">{post.type}</Badge>
      </header>

      <div className="relative flex min-h-44 items-center justify-center bg-[radial-gradient(circle_at_top_left,#f6e7ce,transparent_42%),linear-gradient(135deg,#fff9f0,#f6efe4)] px-5 py-6">
        <PoppyMark className="poppy-float h-16 w-16 text-primary/90" />
        <div className="absolute right-4 top-4 rounded-full bg-white/80 px-3 py-1 text-xs font-semibold text-cream-700 shadow-sm">
          {duration(post.durationSeconds)}{post.distanceMeters ? ` · ${(post.distanceMeters / 1000).toFixed(2)} km` : ''}
        </div>
      </div>

      <div className="space-y-3 px-4 py-4">
        {post.notes ? <p className="text-sm leading-6 text-cream-700">{post.notes}</p> : <p className="text-sm italic text-cream-400">No caption yet.</p>}
        <div className="flex flex-wrap items-center gap-2">
          {REACTIONS.map((reaction) => {
            const active = post.viewerReaction === reaction.type;
            const count = post.reactions[reaction.type] ?? 0;
            return (
              <button
                key={reaction.type}
                type="button"
                disabled={busy}
                onClick={() => react(reaction.type)}
                className={`inline-flex items-center gap-1 rounded-full border px-3 py-1.5 text-xs font-semibold transition ${active ? 'border-primary bg-primary text-white' : 'border-cream-300 bg-cream-50 text-cream-700 hover:border-accent'}`}
              >
                <span>{reaction.icon}</span>
                <span>{reaction.label}{count ? ` ${count}` : ''}</span>
              </button>
            );
          })}
          <button type="button" onClick={toggleComments} className="ml-auto text-xs font-semibold text-cream-500 hover:text-primary">
            {post.commentCount} comments
          </button>
        </div>
        <ErrorMessage message={error} />
        {commentsOpen ? (
          <div className="space-y-3 rounded-2xl bg-cream-50 p-3">
            {comments === null ? <p className="text-xs text-cream-500">Loading comments…</p> : comments.length === 0 ? <p className="text-xs text-cream-500">Be the first to comment.</p> : comments.map((item) => (
              <div key={item.id} className="flex gap-2">
                <Avatar name={item.ownerName} src={item.ownerAvatarUrl} size="sm" className="h-7 w-7" />
                <div className="rounded-2xl bg-white px-3 py-2 text-sm text-cream-800 shadow-sm">
                  <span className="mr-2 font-semibold text-cream-900">{item.ownerName}</span>
                  {item.body}
                </div>
              </div>
            ))}
            <form onSubmit={submitComment} className="flex gap-2">
              <TextInput value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Add a comment…" className="!rounded-full" maxLength={500} />
              <Button type="submit" disabled={busy || !comment.trim()} className="shrink-0">Post</Button>
            </form>
          </div>
        ) : null}
      </div>
    </article>
  );
}

function CityMap({ points }: { points: MapPoints }) {
  const positioned = useMemo(() => {
    const all = [
      ...points.partners.map((partner) => ({ ...partner, kind: 'partner' as const })),
      ...points.activities.map((activity) => ({ ...activity, kind: 'activity' as const, name: activity.petName })),
    ];
    if (all.length === 0) return [];
    const lats = all.map((point) => point.latitude);
    const lngs = all.map((point) => point.longitude);
    const minLat = Math.min(...lats); const maxLat = Math.max(...lats);
    const minLng = Math.min(...lngs); const maxLng = Math.max(...lngs);
    const latSpan = Math.max(0.01, maxLat - minLat);
    const lngSpan = Math.max(0.01, maxLng - minLng);
    return all.map((point) => ({
      ...point,
      left: `${8 + ((point.longitude - minLng) / lngSpan) * 84}%`,
      top: `${82 - ((point.latitude - minLat) / latSpan) * 74}%`,
    }));
  }, [points]);

  return (
    <div className="relative h-80 overflow-hidden rounded-[30px] border border-cream-200 bg-[linear-gradient(135deg,#fff9f0,#f6efe4_55%,#efe3d3)] shadow-[0_30px_80px_-45px_rgba(54,39,20,0.55)]">
      <div className="city-grid absolute inset-0 opacity-60" />
      <div className="absolute inset-0 opacity-40" style={{ backgroundImage: 'radial-gradient(circle at 20% 30%, #e2d2bf 0 2px, transparent 3px), radial-gradient(circle at 70% 60%, #d99c52 0 2px, transparent 3px)' }} />
      <div className="absolute left-5 top-5 rounded-2xl bg-white/85 px-3 py-2 text-xs font-semibold text-cream-700 shadow-sm backdrop-blur">
        Paw City · {points.partners.length} partners · {points.activities.length} activities
      </div>
      <div className="absolute bottom-4 left-4 flex flex-wrap gap-2">
        <span className="inline-flex items-center gap-1.5 rounded-full bg-white/85 px-3 py-1 text-[11px] font-semibold text-cream-700 shadow-sm backdrop-blur">
          <span className="h-2.5 w-2.5 rounded-full bg-secondary" /> Partner
        </span>
        <span className="inline-flex items-center gap-1.5 rounded-full bg-white/85 px-3 py-1 text-[11px] font-semibold text-cream-700 shadow-sm backdrop-blur">
          <span className="h-2.5 w-2.5 rounded-full bg-primary" /> Activity
        </span>
      </div>
      {positioned.length === 0 ? (
        <div className="absolute inset-0 flex items-center justify-center text-sm text-cream-500">No map points yet. Partners and located activities will appear here.</div>
      ) : positioned.map((point) => (
        <div key={`${point.kind}-${point.id}`} className="absolute -translate-x-1/2 -translate-y-1/2 text-center" style={{ left: point.left, top: point.top }}>
          <span className={`mx-auto flex h-10 w-10 items-center justify-center rounded-full border-2 border-white text-white shadow-lg ${point.kind === 'partner' ? 'bg-secondary' : 'bg-primary'}`}>
            <PoppyMark className="h-5 w-5" />
          </span>
          <span className="mt-1 block max-w-24 truncate rounded-full bg-white/85 px-2 py-0.5 text-[10px] font-semibold text-cream-800 shadow-sm">{point.name}</span>
        </div>
      ))}
    </div>
  );
}

export function CityPage() {
  const [posts, setPosts] = useState<ActivityPost[]>([]);
  const [points, setPoints] = useState<MapPoints>({ partners: [], activities: [] });
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const [feed, map] = await Promise.all([listActivityFeed(0, FEED_PAGE_SIZE), listMapPoints()]);
      setPosts(feed);
      setPage(0);
      setHasMore(feed.length === FEED_PAGE_SIZE);
      setPoints(map);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load Paw City');
    } finally {
      setLoading(false);
    }
  }

  async function loadMore() {
    if (loadingMore || !hasMore) {
      return;
    }
    setLoadingMore(true);
    setError(null);
    try {
      const next = await listActivityFeed(page + 1, FEED_PAGE_SIZE);
      setPosts((current) => {
        const seen = new Set(current.map((post) => post.id));
        return [...current, ...next.filter((post) => !seen.has(post.id))];
      });
      setPage((current) => current + 1);
      setHasMore(next.length === FEED_PAGE_SIZE);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load more Paw City posts');
    } finally {
      setLoadingMore(false);
    }
  }

  useEffect(() => { refresh(); }, []);

  function updatePost(updated: ActivityPost) {
    setPosts((current) => current.map((post) => (post.id === updated.id ? updated : post)));
  }

  return (
    <div className="space-y-6">
      <SectionHeader title="Paw City" subtitle="A playful city feed where pet activities become posts, reactions, comments, and map moments." />
      <ErrorMessage message={error} />
      <CityMap points={points} />
      {loading ? <Card><p className="text-sm text-cream-500">Loading Paw City…</p></Card> : posts.length === 0 ? (
        <EmptyState title="The city is quiet" description="Record an activity with a location to publish the first Paw City post." />
      ) : (
        <>
          <div className="grid gap-5 lg:grid-cols-2">
            {posts.map((post) => <ActivityPostCard key={post.id} post={post} onUpdated={updatePost} />)}
          </div>
          {hasMore ? (
            <div className="flex justify-center">
              <Button type="button" variant="secondary" onClick={loadMore} disabled={loadingMore}>
                {loadingMore ? 'Loading…' : 'Load more posts'}
              </Button>
            </div>
          ) : null}
        </>
      )}
    </div>
  );
}
