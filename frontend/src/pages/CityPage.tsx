import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  addActivityComment,
  ApiError,
  getDashboardSummary,
  listActivityComments,
  listActivityFeed,
  listMapPoints,
  listPromotions,
  toggleActivityReaction,
} from '../api/client';
import { useAuth } from '../auth';
import { PawIcon } from '../components/petVisuals';
import { Avatar, Badge, Button, EmptyState, ErrorMessage, PoppyMark, TextInput } from '../components/ui';
import type { ActivityComment, ActivityPost, DashboardSummary, MapPoints, OwnerPromotion } from '../types';

const REACTIONS = [
  { type: 'LIKE', label: 'Like', icon: '♡' },
  { type: 'LOVE', label: 'Love', icon: '♥' },
  { type: 'CHEER', label: 'Cheer', icon: '★' },
] as const;

const FEED_PAGE_SIZE = 12;

const MEDIA_GRADIENTS: Record<string, string> = {
  walk: 'linear-gradient(135deg, #825e31 0%, #d99c52 52%, #b07f43 100%)',
  run: 'linear-gradient(135deg, #5e4424 0%, #b07f43 55%, #d99c52 100%)',
  hike: 'linear-gradient(135deg, #362714 0%, #825e31 58%, #d99c52 100%)',
  bike_ride: 'linear-gradient(135deg, #825e31 0%, #b07f43 45%, #362714 100%)',
  swim: 'linear-gradient(135deg, #5e4424 0%, #d99c52 50%, #825e31 100%)',
  play: 'linear-gradient(135deg, #b07f43 0%, #d99c52 55%, #5e4424 100%)',
};

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

function distanceLabel(meters: number | null): string | null {
  return meters ? `${(meters / 1000).toFixed(2)} km` : null;
}

function titleCase(value: string): string {
  return value.replace(/_/g, ' ').replace(/\b\w/g, (letter) => letter.toUpperCase());
}

function levelInfo(summary: DashboardSummary | null) {
  const activityCount = summary?.activityCountLast7Days ?? 0;
  const distanceMeters = summary?.totalDistanceMetersLast7Days ?? 0;
  const streak = summary?.currentStreakDays ?? 0;
  const xp = activityCount * 120 + Math.round(distanceMeters / 100) * 10 + streak * 25;
  const level = Math.floor(xp / 500) + 1;
  const xpForLevel = 500;
  const xpInLevel = xp % xpForLevel;
  const coins = (summary?.petCount ?? 0) * 50 + activityCount * 15 + streak * 10;
  return { level, xp, xpInLevel, xpForLevel, coins, streak, pets: summary?.petCount ?? 0, activities: activityCount };
}

function CityHero({ summary }: { summary: DashboardSummary | null }) {
  const hud = levelInfo(summary);
  return (
    <section className="game-card relative overflow-hidden p-5 sm:p-6">
      <div className="absolute -right-10 -top-12 h-40 w-40 rounded-full bg-accent/20 blur-3xl" />
      <div className="absolute -left-12 bottom-0 h-36 w-36 rounded-full bg-primary/10 blur-3xl" />
      <PoppyMark className="float-slow pointer-events-none absolute right-8 top-6 h-20 w-20 text-accent/35" />
      <PoppyMark className="poppy-float pointer-events-none absolute right-28 top-16 h-10 w-10 text-primary/25" />
      <div className="relative flex flex-col gap-5 lg:flex-row lg:items-end lg:justify-between">
        <div className="max-w-2xl">
          <Badge tone="accent">Paw City · live city feed</Badge>
          <h1 className="mt-3 font-display text-4xl font-bold tracking-tight text-cream-900 sm:text-5xl">Play, post, and explore with your pack.</h1>
          <p className="mt-3 max-w-xl text-sm leading-6 text-cream-600 sm:text-base">
            Activities become city moments. React, comment, discover partners, and turn every walk into progress.
          </p>
        </div>
        <div className="grid min-w-[280px] gap-3 sm:grid-cols-2 lg:grid-cols-4">
          <div className="hud-chip justify-between"><span>Level</span><strong className="text-primary">{hud.level}</strong></div>
          <div className="hud-chip justify-between"><span>Paw XP</span><strong>{hud.xp}</strong></div>
          <div className="hud-chip justify-between"><span>Paw coins</span><strong>{hud.coins}</strong></div>
          <div className="hud-chip justify-between"><span>Streak</span><strong>{hud.streak}d</strong></div>
        </div>
      </div>
      <div className="relative mt-5">
        <div className="mb-2 flex items-center justify-between text-xs font-semibold text-cream-600">
          <span>Weekly XP</span>
          <span>{hud.xpInLevel} / {hud.xpForLevel}</span>
        </div>
        <div className="xp-track"><div className="xp-fill" style={{ width: `${Math.min(100, Math.round((hud.xpInLevel / hud.xpForLevel) * 100))}%` }} /></div>
      </div>
    </section>
  );
}

function CityMap({ points, compact = false }: { points: MapPoints; compact?: boolean }) {
  const positioned = useMemo(() => {
    const all = [
      ...points.partners.map((partner) => ({ ...partner, kind: 'partner' as const })),
      ...points.activities.map((activity) => ({ ...activity, kind: 'activity' as const, name: activity.petName })),
    ];
    if (all.length === 0) return [];
    const lats = all.map((point) => point.latitude);
    const lngs = all.map((point) => point.longitude);
    const minLat = Math.min(...lats);
    const maxLat = Math.max(...lats);
    const minLng = Math.min(...lngs);
    const maxLng = Math.max(...lngs);
    const latSpan = Math.max(0.01, maxLat - minLat);
    const lngSpan = Math.max(0.01, maxLng - minLng);
    return all.map((point) => ({
      ...point,
      left: `${8 + ((point.longitude - minLng) / lngSpan) * 84}%`,
      top: `${82 - ((point.latitude - minLat) / latSpan) * 74}%`,
    }));
  }, [points]);

  return (
    <div className={`game-card relative overflow-hidden ${compact ? 'h-80' : 'h-[420px]'}`}>
      <div className="city-map absolute inset-0 opacity-80" />
      <div className="absolute inset-x-0 top-0 h-1/3 bg-[linear-gradient(180deg,rgba(255,249,240,0.9),transparent)]" />
      <div className="absolute left-4 top-4 flex items-center gap-2">
        <span className="hud-chip">Paw City map</span>
        <span className="hud-chip">{points.partners.length} partners · {points.activities.length} pins</span>
      </div>
      <div className="absolute bottom-4 left-4 flex flex-wrap gap-2">
        <span className="hud-chip"><span className="h-2.5 w-2.5 rounded-full bg-secondary" /> Partner</span>
        <span className="hud-chip"><span className="h-2.5 w-2.5 rounded-full bg-primary" /> Activity</span>
      </div>
      {positioned.length === 0 ? (
        <div className="absolute inset-0 flex items-center justify-center px-6 text-center text-sm text-cream-600">
          No city pins yet. Registered partners and located Paw City activities will appear here.
        </div>
      ) : positioned.map((point) => (
        <div key={`${point.kind}-${point.id}`} className="absolute -translate-x-1/2 -translate-y-1/2 text-center" style={{ left: point.left, top: point.top }}>
          <span className={`relative mx-auto flex h-11 w-11 items-center justify-center rounded-full border-2 border-white text-white shadow-xl ${point.kind === 'partner' ? 'bg-secondary' : 'bg-primary'}`}>
            {point.kind === 'activity' ? <span className="map-pulse absolute inset-0 rounded-full bg-accent/50" /> : null}
            <PawIcon className="relative h-5 w-5" />
          </span>
          <span className="mt-1 block max-w-28 truncate rounded-full bg-white/90 px-2 py-0.5 text-[10px] font-bold text-cream-900 shadow-sm">{point.name}</span>
        </div>
      ))}
    </div>
  );
}

function QuestPanel({ summary, points, sharedPostCount }: { summary: DashboardSummary | null; points: MapPoints; sharedPostCount: number }) {
  const hud = levelInfo(summary);
  const cityPins = points.partners.length + points.activities.length;
  const quests = [
    { title: 'First miles', description: 'Record 3 activities this week', progress: Math.min(hud.activities / 3, 1), reward: '+120 XP' },
    { title: 'City explorer', description: 'Discover 5 partner or activity pins', progress: Math.min(cityPins / 5, 1), reward: '+90 XP' },
    { title: 'Social pack', description: 'Share 3 activities to Paw City', progress: Math.min(sharedPostCount / 3, 1), reward: '+75 XP' },
    { title: 'Streak keeper', description: 'Reach a 7-day activity streak', progress: Math.min(hud.streak / 7, 1), reward: '+150 XP' },
  ];

  return (
    <section className="game-card p-5">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cream-500">Trainer quests</p>
          <h2 className="mt-1 font-display text-2xl font-bold text-cream-900">Daily pack missions</h2>
        </div>
        <PoppyMark className="h-10 w-10 text-accent" />
      </div>
      <div className="mt-4 space-y-3">
        {quests.map((quest) => {
          const complete = quest.progress >= 1;
          return (
            <article key={quest.title} className="quest-card">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-bold text-cream-900">{quest.title}</p>
                  <p className="mt-1 text-sm text-cream-500">{quest.description}</p>
                </div>
                {complete ? <Badge tone="success">Done</Badge> : <Badge tone="accent">{quest.reward}</Badge>}
              </div>
              <div className="xp-track mt-3"><div className="xp-fill" style={{ width: `${Math.round(quest.progress * 100)}%` }} /></div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

function ActivityPostCard({ post, onUpdated }: { post: ActivityPost; onUpdated: (post: ActivityPost) => void }) {
  const [comments, setComments] = useState<ActivityComment[] | null>(null);
  const [commentsOpen, setCommentsOpen] = useState(false);
  const [comment, setComment] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const likeCount = Object.values(post.reactions).reduce((total, count) => total + count, 0);
  const mediaGradient = MEDIA_GRADIENTS[post.type] ?? MEDIA_GRADIENTS.play;
  const isDog = post.petSpecies.toLowerCase().includes('dog');

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
    <article className="game-card feed-enter overflow-hidden">
      <header className="flex items-center gap-3 px-4 py-3">
        <Avatar name={post.ownerName} src={post.ownerAvatarUrl} className="h-11 w-11" />
        <div className="min-w-0 flex-1">
          <p className="truncate font-bold text-cream-900">{post.ownerName}</p>
          <p className="truncate text-xs text-cream-500">
            {post.petName} · {titleCase(post.petSpecies)} · {post.latitude != null && post.longitude != null ? 'Pinned in Paw City' : timeAgo(post.startedAt)}
          </p>
        </div>
        <Badge tone="accent">{titleCase(post.type)}</Badge>
      </header>

      <div className="post-media border-y border-cream-100" style={isDog ? undefined : { background: mediaGradient }}>
        {isDog ? <img src="/assets/dog-walk.jpg" alt="Dog activity" className="absolute inset-0 h-full w-full object-cover" /> : null}
        <div className="absolute inset-0 bg-gradient-to-t from-dark/65 via-dark/15 to-transparent" />
        <div className="absolute left-4 top-4 flex items-center gap-2">
          <span className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-white/90 text-primary shadow-sm">
            <PawIcon className="h-5 w-5" />
          </span>
          <Badge tone="accent">{titleCase(post.type)}</Badge>
        </div>
        <div className="absolute right-4 top-4 rounded-full bg-white/85 px-3 py-1 text-xs font-bold text-cream-800 shadow-sm backdrop-blur">
          {duration(post.durationSeconds)}{distanceLabel(post.distanceMeters) ? ` · ${distanceLabel(post.distanceMeters)}` : ''}
        </div>
        <div className="absolute bottom-4 left-4 right-4 text-white">
          <p className="font-display text-xl font-bold drop-shadow">{post.petName}'s {titleCase(post.type).toLowerCase()}</p>
          <p className="text-sm text-white/85">{timeAgo(post.startedAt)}</p>
        </div>
      </div>

      <div className="px-4 py-3">
        <div className="flex items-center gap-1">
          {REACTIONS.map((reaction) => {
            const active = post.viewerReaction === reaction.type;
            const count = post.reactions[reaction.type] ?? 0;
            return (
              <button
                key={reaction.type}
                type="button"
                title={reaction.label}
                disabled={busy}
                onClick={() => react(reaction.type)}
                className={`inline-flex items-center gap-1 rounded-full px-3 py-2 text-sm font-bold transition ${active ? 'bg-primary text-white shadow-sm' : 'text-cream-600 hover:bg-accent-soft hover:text-primary'}`}
              >
                <span>{reaction.icon}</span>
                {count > 0 ? <span>{count}</span> : null}
              </button>
            );
          })}
          <button type="button" onClick={toggleComments} className="ml-auto rounded-full px-3 py-2 text-sm font-bold text-cream-600 hover:bg-accent-soft hover:text-primary">
            {post.commentCount} comments
          </button>
        </div>

        <p className="mt-2 text-sm text-cream-500"><strong className="text-cream-900">{likeCount}</strong> paw reactions</p>
        {post.notes ? (
          <p className="mt-2 text-sm leading-6 text-cream-800"><strong className="font-bold">{post.ownerName}</strong> {post.notes}</p>
        ) : null}
        <ErrorMessage message={error} />

        {commentsOpen ? (
          <div className="mt-3 space-y-3 rounded-3xl bg-cream-50 p-3">
            {comments === null ? <p className="text-xs text-cream-500">Loading comments…</p> : comments.length === 0 ? <p className="text-xs text-cream-500">Be the first to comment.</p> : comments.map((item) => (
              <div key={item.id} className="flex gap-2">
                <Avatar name={item.ownerName} src={item.ownerAvatarUrl} size="sm" className="h-7 w-7" />
                <div className="rounded-2xl rounded-tl-sm bg-white px-3 py-2 text-sm text-cream-800 shadow-sm">
                  <span className="mr-2 font-bold text-cream-900">{item.ownerName}</span>
                  {item.body}
                </div>
              </div>
            ))}
            <form onSubmit={submitComment} className="flex gap-2">
              <TextInput value={comment} onChange={(event) => setComment(event.target.value)} placeholder="Add a comment…" className="!rounded-full" maxLength={500} />
              <Button type="submit" disabled={busy || !comment.trim()} className="shrink-0">Post</Button>
            </form>
          </div>
        ) : post.commentCount > 0 ? (
          <button type="button" onClick={toggleComments} className="mt-2 text-xs font-semibold text-cream-500 hover:text-primary">
            View all {post.commentCount} comments
          </button>
        ) : null}
      </div>
    </article>
  );
}

function NearbyPromotions({ promotions }: { promotions: OwnerPromotion[] }) {
  if (promotions.length === 0) {
    return null;
  }
  return (
    <section className="quest-card">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-xs font-semibold uppercase tracking-[0.18em] text-cream-500">Nearby promotions</p>
          <h2 className="mt-1 font-display text-xl font-bold text-cream-900">Partner offers</h2>
        </div>
        <Badge tone="accent">Ads</Badge>
      </div>
      <div className="mt-4 space-y-3">
        {promotions.slice(0, 4).map((promotion) => {
          const website = promotion.partnerWebsite?.trim();
          const href = website ? (/^https?:\/\//i.test(website) ? website : `https://${website}`) : null;
          return (
            <article key={promotion.campaignId} className="rounded-2xl border border-cream-200 bg-white/80 p-3">
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="text-xs font-bold uppercase tracking-[0.16em] text-primary">{promotion.partnerName}</p>
                  <h3 className="mt-1 font-bold text-cream-900">{promotion.title}</h3>
                </div>
                <Badge tone="danger">Promo</Badge>
              </div>
              <p className="mt-2 text-sm leading-5 text-cream-600">{promotion.description}</p>
              <div className="mt-3 flex items-center justify-between gap-3">
                <span className="text-xs font-semibold text-cream-500">{promotion.expiresAt ? `Until ${new Date(promotion.expiresAt).toLocaleDateString(undefined, { month: 'short', day: 'numeric' })}` : 'No expiry'}</span>
                {href ? (
                  <a href={href} target="_blank" rel="noreferrer" className="rounded-xl bg-primary px-3 py-1.5 text-xs font-bold text-white hover:bg-secondary">
                    {promotion.callToAction}
                  </a>
                ) : (
                  <span className="rounded-xl bg-cream-200 px-3 py-1.5 text-xs font-bold text-cream-600">{promotion.callToAction}</span>
                )}
              </div>
            </article>
          );
        })}
      </div>
    </section>
  );
}

export function CityPage() {
  const { owner } = useAuth();
  const [posts, setPosts] = useState<ActivityPost[]>([]);
  const [points, setPoints] = useState<MapPoints>({ partners: [], activities: [] });
  const [promotions, setPromotions] = useState<OwnerPromotion[]>([]);
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(0);
  const [hasMore, setHasMore] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const [feed, map, dashboard, promoList] = await Promise.all([
        listActivityFeed(0, FEED_PAGE_SIZE),
        listMapPoints(),
        getDashboardSummary().catch(() => null),
        listPromotions().catch(() => []),
      ]);
      setPosts(feed);
      setPage(0);
      setHasMore(feed.length === FEED_PAGE_SIZE);
      setPoints(map);
      setPromotions(promoList);
      setSummary(dashboard);
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

  const sharedPostCount = posts.filter((post) => post.ownerId === owner?.id).length;

  return (
    <div className="space-y-6">
      <CityHero summary={summary} />
      <ErrorMessage message={error} />
      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-5">
          {loading ? (
            <div className="game-card p-8 text-sm text-cream-500">Loading Paw City…</div>
          ) : posts.length === 0 ? (
            <EmptyState
              title="The city is quiet"
              description="Record an activity with a location and share it to Paw City to publish the first post."
            />
          ) : (
            <>
              {posts.map((post) => <ActivityPostCard key={post.id} post={post} onUpdated={updatePost} />)}
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
        <aside className="space-y-5 xl:sticky xl:top-28 xl:self-start">
          <CityMap points={points} compact />
          <NearbyPromotions promotions={promotions} />
          <QuestPanel summary={summary} points={points} sharedPostCount={sharedPostCount} />
        </aside>
      </div>
    </div>
  );
}
