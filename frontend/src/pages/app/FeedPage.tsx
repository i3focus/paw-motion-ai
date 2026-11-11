import { useCallback, useEffect, useRef, useState } from 'react';
import { getFeed, getStories, viewStory, type FeedCard, type StoryGroup } from '../../api/petlife';
import { EmptyFeed, FeedItemCard } from '../../components/FeedCards';
import { Avatar, LoadingState, StoryRing } from '../../components/ui';

function StoryRail({ groups, onOpen }: { groups: StoryGroup[]; onOpen: (g: StoryGroup) => void }) {
  if (groups.length === 0) return null;
  return (
    <div className="flex gap-4 overflow-x-auto pb-2" role="list" aria-label="Stories">
      {groups.map((g) => {
        const state = g.own ? 'add' : g.allViewed ? 'seen' : (g.stories ?? []).some((s) => !s.viewed) ? 'new' : 'seen';
        return (
          <button key={g.petId ?? g.ownerId} type="button" onClick={() => onOpen(g)} aria-label={`${g.petName}'s stories`}>
            <StoryRing state={state} label={g.petName ?? 'You'}>
              <Avatar name={g.petName ?? '?'} imageUrl={g.petAvatarUrl} className="h-full w-full" />
            </StoryRing>
          </button>
        );
      })}
    </div>
  );
}

function StoryViewer({ group, onClose }: { group: StoryGroup; onClose: () => void }) {
  const [index, setIndex] = useState(0);
  const stories = group.stories ?? [];
  const story = stories[index];
  useEffect(() => {
    if (story?.id) viewStory(story.id).catch(() => {});
  }, [story?.id]);
  if (!story) return null;
  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-overlay p-4" role="dialog" aria-modal="true" aria-label={`${group.petName}'s story`} onClick={onClose}>
      <div className="relative w-full max-w-sm overflow-hidden rounded-card bg-teal-dark shadow-raised" onClick={(e) => e.stopPropagation()}>
        <div className="flex items-center gap-2 p-3 text-white">
          <Avatar name={group.petName ?? '?'} imageUrl={group.petAvatarUrl} size="sm" />
          <p className="flex-1 font-extrabold">{group.petName}</p>
          <button type="button" onClick={onClose} aria-label="Close story" className="grid h-9 w-9 place-items-center rounded-full bg-white/20 font-black">×</button>
        </div>
        {story.mediaUrl ? <img src={story.mediaUrl} alt="" className="h-[60vh] w-full object-cover" /> : <div className="grid h-[40vh] place-items-center text-6xl">🐾</div>}
        {story.caption ? <p className="p-4 text-body font-semibold text-white">{story.caption}</p> : null}
        <div className="flex justify-between p-3">
          <button type="button" disabled={index === 0} onClick={() => setIndex(index - 1)} className="min-h-11 rounded-pill bg-white/20 px-4 font-extrabold text-white disabled:opacity-40">Back</button>
          <button type="button" onClick={() => index + 1 < stories.length ? setIndex(index + 1) : onClose()} className="min-h-11 rounded-pill bg-coral px-4 font-extrabold text-coral-ink">
            {index + 1 < stories.length ? 'Next' : 'Done'}
          </button>
        </div>
      </div>
    </div>
  );
}

export function FeedPage() {
  const [cards, setCards] = useState<FeedCard[] | null>(null);
  const [stories, setStories] = useState<StoryGroup[]>([]);
  const [page, setPage] = useState(0);
  const [hasNext, setHasNext] = useState(false);
  const [loadingMore, setLoadingMore] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [viewer, setViewer] = useState<StoryGroup | null>(null);
  const requestId = useRef(0);

  const load = useCallback((targetPage: number, append: boolean) => {
    const id = ++requestId.current;
    setLoadingMore(append);
    getFeed(targetPage, 20)
      .then((res) => {
        if (id !== requestId.current) return;
        setCards((prev) => (append ? [...(prev ?? []), ...(res.items ?? [])] : (res.items ?? [])));
        setPage(targetPage);
        setHasNext(Boolean(res.hasNext));
        setError(null);
      })
      .catch((e) => { if (id === requestId.current) { setError(e instanceof Error ? e.message : 'Could not load the feed'); setCards((c) => c ?? []); } })
      .finally(() => { if (id === requestId.current) setLoadingMore(false); });
  }, []);

  useEffect(() => {
    load(0, false);
    getStories().then(setStories).catch(() => setStories([]));
  }, [load]);

  useEffect(() => {
    const refresh = () => { load(0, false); getStories().then(setStories).catch(() => {}); };
    window.addEventListener('petlife:feed-refresh', refresh);
    return () => window.removeEventListener('petlife:feed-refresh', refresh);
  }, [load]);

  if (cards === null) return <LoadingState label="Loading the feed…" />;

  return (
    <div className="mx-auto max-w-2xl space-y-5">
      <StoryRail groups={stories} onOpen={setViewer} />
      {error ? <p role="alert" className="pl-card p-4 text-body font-bold text-danger">{error}</p> : null}
      {cards.length === 0 ? <EmptyFeed /> : cards.map((c) => <FeedItemCard key={c.id} card={c} />)}
      {hasNext ? (
        <button type="button" onClick={() => load(page + 1, true)} disabled={loadingMore}
          className="min-h-12 w-full rounded-pill bg-white font-extrabold text-teal shadow-card hover:bg-teal-light disabled:opacity-50">
          {loadingMore ? 'Loading…' : 'Load more'}
        </button>
      ) : null}
      {viewer ? <StoryViewer group={viewer} onClose={() => setViewer(null)} /> : null}
    </div>
  );
}
