import { useEffect, useMemo, useState, type FormEvent } from 'react';
import {
  addActivityComment,
  ApiError,
  getDashboardSummary,
  listActivityComments,
  listActivityFeed,
  listActivities,
  listPets,
  listPromotions,
  listRecommendations,
  toggleActivityReaction,
} from '../api/client';
import { useAuth } from '../auth';
import { PetAvatar, PawIcon } from '../components/petVisuals';
import { Avatar, Badge, Button, EmptyState, ErrorMessage, PawMark, TextInput } from '../components/ui';
import type { Activity, ActivityComment, ActivityPost, DashboardSummary, OwnerPromotion, OwnerRecommendation, Pet } from '../types';

const REACTIONS = [
  { type: 'LIKE', label: 'Like', icon: '♡' },
  { type: 'LOVE', label: 'Love', icon: '♥' },
  { type: 'CHEER', label: 'Cheer', icon: '★' },
] as const;

const FEED_PAGE_SIZE = 12;

const MEDIA_GRADIENTS: Record<string, string> = {
  walk: 'var(--color-teal-light)',
  run: 'var(--color-mint)',
  hike: 'var(--color-sand-deep)',
  bike_ride: 'var(--color-amber-soft)',
  swim: 'var(--color-coral-soft)',
  play: 'var(--color-sand)',
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

function sameDay(value: string, date: Date): boolean {
  const activityDate = new Date(value);
  return activityDate.getFullYear() === date.getFullYear()
    && activityDate.getMonth() === date.getMonth()
    && activityDate.getDate() === date.getDate();
}

function PostAvatar({ post }: { post: ActivityPost }) {
  if (post.petSpecies.toLowerCase().includes('dog')) {
    return (
      <span className="relative inline-flex h-11 w-11 shrink-0 overflow-hidden rounded-full border border-line bg-sand">
        <img src="/assets/dog-portrait.jpg" alt={`${post.petName} avatar`} className="h-full w-full object-cover" />
        <PawIcon className="absolute bottom-0.5 right-0.5 h-3.5 w-3.5 text-white drop-shadow" />
      </span>
    );
  }
  return (
    <span className="inline-flex h-11 w-11 shrink-0 items-center justify-center rounded-full bg-teal text-white shadow-sm">
      <PawIcon className="h-5 w-5" />
    </span>
  );
}

function StoriesRow({ pets, ownerName, ownerAvatarUrl }: { pets: Pet[]; ownerName: string; ownerAvatarUrl: string | null }) {
  return (
    <section className="pl-card p-4">
      <div className="flex gap-5 overflow-x-auto pb-1">
        <div className="flex w-16 shrink-0 flex-col items-center gap-2">
          <span className="relative inline-flex h-16 w-16 items-center justify-center rounded-full border-2 border-dashed border-amber bg-cream">
            {ownerAvatarUrl ? <img src={ownerAvatarUrl} alt={ownerName} className="h-full w-full rounded-full object-cover" /> : <PawIcon className="h-7 w-7 text-teal" />}
            <span className="absolute -bottom-1 -right-1 inline-flex h-6 w-6 items-center justify-center rounded-full bg-teal text-white shadow-sm">+</span>
          </span>
          <span className="w-full truncate text-center text-label font-semibold text-muted">Your story</span>
        </div>
        {pets.map((pet) => (
          <div key={pet.id} className="flex w-16 shrink-0 flex-col items-center gap-2">
            <span className="rounded-full bg-teal-light p-1">
              <PetAvatar pet={pet} className="h-14 w-14" />
            </span>
            <span className="w-full truncate text-center text-label font-semibold text-muted">{pet.name}</span>
          </div>
        ))}
      </div>
    </section>
  );
}

function PetWeekCard({ summary, pets, activities }: { summary: DashboardSummary | null; pets: Pet[]; activities: Activity[] }) {
  const recent = activities.length > 0 ? activities : (summary?.recentActivities ?? []);
  const days = useMemo(() => {
    return Array.from({ length: 7 }, (_, index) => {
      const date = new Date();
      date.setDate(date.getDate() - (6 - index));
      return {
        date,
        label: ['S', 'M', 'T', 'W', 'T', 'F', 'S'][date.getDay()],
        count: recent.filter((activity) => sameDay(activity.startedAt, date)).length,
      };
    });
  }, [recent]);
  const maxCount = Math.max(1, ...days.map((day) => day.count));
  const chartedCount = days.reduce((total, day) => total + day.count, 0);
  const weekIsComplete = activities.length > 0 || chartedCount >= (summary?.activityCountLast7Days ?? 0);
  const petCount = summary?.petCount ?? pets.length;

  return (
    <section className="pl-card-sand">
      <div className="flex items-start justify-between gap-3">
        <div>
          <p className="text-label font-semibold uppercase tracking-[0.18em] text-muted">Pack week</p>
          <h2 className="mt-1 font-display text-2xl font-bold text-ink">{petCount === 1 ? 'Your pet' : 'Your pets'}&apos; week</h2>
        </div>
        <PawMark className="h-9 w-9 text-amber-ink" />
      </div>
      <div className="mt-4 grid grid-cols-3 gap-2">
        <div className="rounded-2xl bg-cream p-3 text-center">
          <p className="text-lg font-bold text-ink">{summary?.activityCountLast7Days ?? 0}</p>
          <p className="text-caption font-semibold uppercase tracking-wide text-muted">Adventures</p>
        </div>
        <div className="rounded-2xl bg-cream p-3 text-center">
          <p className="text-lg font-bold text-ink">{((summary?.totalDistanceMetersLast7Days ?? 0) / 1000).toFixed(1)}</p>
          <p className="text-caption font-semibold uppercase tracking-wide text-muted">Km</p>
        </div>
        <div className="rounded-2xl bg-cream p-3 text-center">
          <p className="text-lg font-bold text-ink">{summary?.currentStreakDays ?? 0}</p>
          <p className="text-caption font-semibold uppercase tracking-wide text-muted">Streak</p>
        </div>
      </div>
      {weekIsComplete ? (
        <div className="mt-5 flex h-24 items-end gap-2">
          {days.map((day) => (
            <div key={day.date.toISOString()} className="flex flex-1 flex-col items-center gap-1">
              <div className="flex h-16 w-full items-end rounded-2xl bg-sand">
                <div className="w-full rounded-2xl bg-teal" style={{ height: `${Math.max(12, (day.count / maxCount) * 100)}%` }} />
              </div>
              <span className="text-caption font-bold text-muted">{day.label}</span>
            </div>
          ))}
        </div>
      ) : (
        <div className="mt-5 flex h-24 items-center justify-center rounded-2xl bg-sand text-body font-semibold text-muted">
          Weekly detail unavailable
        </div>
      )}
    </section>
  );
}

function AiSuggestionCard({ recommendations }: { recommendations: OwnerRecommendation[] }) {
  const suggestions = recommendations.length > 0
    ? recommendations.slice(0, 3).map((recommendation) => recommendation.recommendedTopics[0] ?? recommendation.campaignTitle)
    : ['Find pet-friendly businesses', 'Summarize this week', 'Discover partner offers'];

  return (
    <section className="pl-card-sand">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-label font-semibold uppercase tracking-[0.18em] text-muted">PetLife AI</p>
          <h2 className="mt-1 font-display text-xl font-bold text-ink">Suggested for your pack</h2>
        </div>
        <Badge tone="accent">AI</Badge>
      </div>
      <div className="mt-4 space-y-2">
        {suggestions.map((suggestion) => (
          <div key={suggestion} className="flex items-center gap-3 rounded-2xl border border-line bg-white/80 px-3 py-3">
            <span className="inline-flex h-9 w-9 shrink-0 items-center justify-center rounded-2xl bg-teal-light text-teal">
              <PawIcon className="h-4 w-4" />
            </span>
            <p className="text-body font-semibold text-ink">{suggestion}</p>
          </div>
        ))}
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
    <article className="pl-card pl-fade-in overflow-hidden">
      <header className="flex items-center gap-3 px-4 py-3">
        <PostAvatar post={post} />
        <div className="min-w-0 flex-1">
          <p className="truncate font-bold text-ink">{post.petName}</p>
          <p className="truncate text-label text-muted">
            {post.ownerName} · {titleCase(post.petSpecies)} · {post.latitude != null && post.longitude != null ? 'PETLIFE' : timeAgo(post.startedAt)}
          </p>
        </div>
        <Badge tone="accent">{titleCase(post.type)}</Badge>
      </header>

      <div className="pl-media border-y border-line" style={isDog ? undefined : { background: mediaGradient }}>
        {isDog ? <img src="/assets/dog-walk.jpg" alt="Dog activity" className="absolute inset-0 h-full w-full object-cover" /> : null}
        <div className="absolute inset-0 bg-gradient-to-t from-teal-dark/65 via-teal-dark/15 to-transparent" />
        <div className="absolute left-4 top-4 flex items-center gap-2">
          <span className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-white/90 text-teal shadow-sm">
            <PawIcon className="h-5 w-5" />
          </span>
          <Badge tone="accent">{titleCase(post.type)}</Badge>
        </div>
        <div className="absolute right-4 top-4 rounded-full bg-white/85 px-3 py-1 text-label font-bold text-ink shadow-sm backdrop-blur">
          {duration(post.durationSeconds)}{distanceLabel(post.distanceMeters) ? ` · ${distanceLabel(post.distanceMeters)}` : ''}
        </div>
        <div className="absolute bottom-4 left-4 right-4 text-white">
          <p className="font-display text-xl font-bold drop-shadow">{post.petName}&apos;s {titleCase(post.type).toLowerCase()}</p>
          <p className="text-body text-white/85">{timeAgo(post.startedAt)}</p>
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
                className={`inline-flex items-center gap-1 rounded-full px-3 py-2 text-body font-bold transition ${active ? 'bg-teal text-white shadow-sm' : 'text-muted hover:bg-teal-light hover:text-teal'}`}
              >
                <span>{reaction.icon}</span>
                {count > 0 ? <span>{count}</span> : null}
              </button>
            );
          })}
          <button type="button" onClick={toggleComments} className="ml-auto rounded-full px-3 py-2 text-body font-bold text-muted hover:bg-teal-light hover:text-teal">
            {post.commentCount} comments
          </button>
        </div>

        <p className="mt-2 text-body text-muted"><strong className="text-ink">{likeCount}</strong> paw reactions</p>
        {post.notes ? (
          <p className="mt-2 text-body leading-6 text-ink"><strong className="font-bold">{post.ownerName}</strong> {post.notes}</p>
        ) : null}
        <ErrorMessage message={error} />

        {commentsOpen ? (
          <div className="mt-3 space-y-3 rounded-3xl bg-cream p-3">
            {comments === null ? <p className="text-label text-muted">Loading comments…</p> : comments.length === 0 ? <p className="text-label text-muted">Be the first to comment.</p> : comments.map((item) => (
              <div key={item.id} className="flex gap-2">
                <Avatar name={item.ownerName} src={item.ownerAvatarUrl} size="sm" className="h-7 w-7" />
                <div className="rounded-2xl rounded-tl-sm bg-white px-3 py-2 text-body text-ink shadow-sm">
                  <span className="mr-2 font-bold text-ink">{item.ownerName}</span>
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
          <button type="button" onClick={toggleComments} className="mt-2 text-label font-semibold text-muted hover:text-teal">
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
    <section className="pl-card-sand">
      <div className="flex items-center justify-between gap-3">
        <div>
          <p className="text-label font-semibold uppercase tracking-[0.18em] text-muted">Nearby promotions</p>
          <h2 className="mt-1 font-display text-xl font-bold text-ink">Partner offers</h2>
        </div>
        <Badge tone="accent">Ads</Badge>
      </div>
      <div className="mt-4 space-y-3">
        {promotions.slice(0, 3).map((promotion) => {
          const website = promotion.partnerWebsite?.trim();
          const href = website ? (/^https?:\/\//i.test(website) ? website : `https://${website}`) : null;
          return (
            <article key={promotion.campaignId} className="overflow-hidden rounded-3xl border border-line bg-white/85">
              <div className="relative flex h-20 items-center justify-center" style={{ background: 'var(--color-teal-light)' }}>
                <PawMark className="h-10 w-10 text-white/85" />
                <div className="absolute left-3 top-3 flex gap-1.5">
                  <Badge tone="accent">Pet-friendly</Badge>
                  <Badge tone="danger">Promo</Badge>
                </div>
              </div>
              <div className="p-3">
                <p className="text-label font-bold uppercase tracking-[0.16em] text-teal">{promotion.partnerName}</p>
                <h3 className="mt-1 font-bold text-ink">{promotion.title}</h3>
                <p className="mt-1 line-clamp-2 text-body leading-5 text-muted">{promotion.description}</p>
                <div className="mt-3 flex items-center justify-between gap-3">
                  <span className="text-label font-semibold text-muted">{promotion.targetAudience}</span>
                  {href ? (
                    <a href={href} target="_blank" rel="noreferrer" className="rounded-xl bg-teal px-3 py-1.5 text-label font-bold text-white hover:bg-teal-dark">
                      {promotion.callToAction}
                    </a>
                  ) : (
                    <span className="rounded-xl bg-sand-deep px-3 py-1.5 text-label font-bold text-muted">{promotion.callToAction}</span>
                  )}
                </div>
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
  const [activities, setActivities] = useState<Activity[]>([]);
  const [pets, setPets] = useState<Pet[]>([]);
  const [promotions, setPromotions] = useState<OwnerPromotion[]>([]);
  const [recommendations, setRecommendations] = useState<OwnerRecommendation[]>([]);
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
      const [feed, activityList, petList, dashboard, promoList, recommendationList] = await Promise.all([
        listActivityFeed(0, FEED_PAGE_SIZE),
        listActivities().catch(() => [] as Activity[]),
        listPets().catch(() => [] as Pet[]),
        getDashboardSummary().catch(() => null),
        listPromotions().catch(() => [] as OwnerPromotion[]),
        listRecommendations().catch(() => [] as OwnerRecommendation[]),
      ]);
      setPosts(feed);
      setPage(0);
      setHasMore(feed.length === FEED_PAGE_SIZE);
      setActivities(activityList);
      setPets(petList);
      setPromotions(promoList);
      setRecommendations(recommendationList);
      setSummary(dashboard);
    } catch (caught) {
      setError(caught instanceof ApiError ? caught.message : 'Unable to load PETLIFE');
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
      setError(caught instanceof ApiError ? caught.message : 'Unable to load more PETLIFE posts');
    } finally {
      setLoadingMore(false);
    }
  }

  useEffect(() => { refresh(); }, []);

  function updatePost(updated: ActivityPost) {
    setPosts((current) => current.map((post) => (post.id === updated.id ? updated : post)));
  }

  return (
    <div className="mx-auto max-w-7xl space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-4">
        <div>
          <Badge tone="accent">Feed</Badge>
          <h1 className="mt-3 font-display text-4xl font-bold tracking-tight text-ink">Welcome back, {owner?.displayName ?? 'trainer'}.</h1>
          <p className="mt-2 max-w-2xl text-body leading-6 text-muted">
            Stories, activity posts, partner offers, and AI suggestions for your pack.
          </p>
        </div>
        <div className="pl-chip">{pets.length} pet{pets.length === 1 ? '' : 's'} · {summary?.activityCountLast7Days ?? 0} adventures this week</div>
      </div>

      <ErrorMessage message={error} />

      <div className="grid gap-6 xl:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-5">
          <StoriesRow pets={pets} ownerName={owner?.displayName ?? 'You'} ownerAvatarUrl={owner?.avatarUrl ?? null} />
          {loading ? (
            <div className="pl-card p-8 text-body text-muted">Loading PETLIFE…</div>
          ) : posts.length === 0 ? (
            <EmptyState
              title="The city is quiet"
              description="Record an activity with a location and share it to PETLIFE to publish the first post."
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
          <PetWeekCard summary={summary} pets={pets} activities={activities} />
          <AiSuggestionCard recommendations={recommendations} />
          <NearbyPromotions promotions={promotions} />
        </aside>
      </div>
    </div>
  );
}
