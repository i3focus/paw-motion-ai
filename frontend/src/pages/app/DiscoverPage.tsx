import { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { getChallenges, getDiscover, getLeaderboard, joinChallenge, joinCommunity, rsvpEvent, type Challenge, type Discover, type Leaderboard } from '../../api/petlife';
import { listPets } from '../../api/client';
import type { Pet } from '../../types';
import { Avatar, Badge, Button, Chip, LoadingState, SponsoredLabel } from '../../components/ui';

export function DiscoverPage() {
  const [discover, setDiscover] = useState<Discover | null>(null);
  const [board, setBoard] = useState<Leaderboard | null>(null);
  const [challenges, setChallenges] = useState<Challenge[]>([]);
  const [pets, setPets] = useState<Pet[]>([]);
  const [scope, setScope] = useState<'neighborhood' | 'global'>('neighborhood');
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    navigator.geolocation?.getCurrentPosition(
      (p) => getDiscover(p.coords.latitude, p.coords.longitude).then(setDiscover).catch((e) => setError(e instanceof Error ? e.message : 'Could not load Discover')),
      () => getDiscover().then(setDiscover).catch((e) => setError(e instanceof Error ? e.message : 'Could not load Discover')),
      { timeout: 5000 },
    );
    listPets().then(setPets).catch(() => setPets([]));
    getChallenges().then(setChallenges).catch(() => setChallenges([]));
  }, []);

  useEffect(() => { getLeaderboard(scope).then(setBoard).catch(() => setBoard({ entries: [] })); }, [scope]);

  if (!discover && !error) return <LoadingState label="Loading Discover…" />;

  return (
    <div className="space-y-8">
      <h1 className="text-heading">Discover</h1>
      {error ? <p role="alert" className="pl-card p-4 font-bold text-danger">{error}</p> : null}

      {(discover?.sponsored ?? []).length > 0 ? (
        <section>
          <h2 className="mb-3 text-title">Near you</h2>
          <div className="grid gap-3 sm:grid-cols-2">
            {discover!.sponsored!.map((s) => (
              <div key={s.campaignId ?? s.partnerId} className="pl-card p-4 ring-2 ring-inset ring-amber">
                <div className="flex items-center justify-between"><SponsoredLabel />{s.verified ? <Badge tone="success">✓ Verified</Badge> : null}</div>
                <p className="mt-2 font-extrabold text-teal-dark">{s.title ?? s.partnerName}</p>
                <p className="text-body text-muted">{s.description}{s.partnerName && s.title ? ` · ${s.partnerName}` : ''}</p>
                {s.ctaLabel && s.ctaUrl ? <a href={s.ctaUrl} className="mt-2 inline-flex min-h-11 items-center rounded-pill bg-coral px-4 text-label font-black text-coral-ink">{s.ctaLabel}</a> : null}
              </div>
            ))}
          </div>
        </section>
      ) : null}

      <section>
        <h2 className="mb-3 text-title">Communities</h2>
        <div className="grid gap-3 sm:grid-cols-2 lg:grid-cols-3">
          {(discover?.communities ?? []).map((c) => (
            <CommunityCard key={c.id} community={c} />
          ))}
          {(discover?.communities ?? []).length === 0 ? <p className="text-body text-muted">No communities yet — start one for your breed or neighborhood!</p> : null}
        </div>
      </section>

      <section>
        <h2 className="mb-3 text-title">Upcoming events</h2>
        <div className="space-y-3">
          {(discover?.events ?? []).map((e) => (
            <div key={e.id} className="pl-card flex flex-wrap items-center gap-4 p-4">
              <div className="min-w-0 flex-1">
                <p className="font-extrabold text-ink">{e.title}</p>
                <p className="text-caption text-muted">{e.communityName ? `${e.communityName} · ` : ''}{e.locationName ?? ''} · {e.startsAt ? new Date(e.startsAt).toLocaleString(undefined, { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' }) : ''}</p>
                <p className="text-caption text-muted">{e.goingCount ?? 0} going{e.interestedCount ? ` · ${e.interestedCount} interested` : ''}</p>
              </div>
              {e.viewerRsvp ? <Badge tone="success">{e.viewerRsvp === 'GOING' ? 'Going ✓' : 'Interested ✓'}</Badge> : (
                <div className="flex gap-2">
                  <Button size="sm" onClick={() => rsvpEvent(e.id!, 'GOING').then(() => { e.viewerRsvp = 'GOING'; setDiscover({ ...discover! }); }).catch(() => {})}>Going</Button>
                  <Button size="sm" variant="ghost" onClick={() => rsvpEvent(e.id!, 'INTERESTED').then(() => { e.viewerRsvp = 'INTERESTED'; setDiscover({ ...discover! }); }).catch(() => {})}>Interested</Button>
                </div>
              )}
            </div>
          ))}
          {(discover?.events ?? []).length === 0 ? <p className="text-body text-muted">No upcoming events in your communities.</p> : null}
        </div>
      </section>

      <section>
        <h2 className="mb-3 text-title">Weekly challenges</h2>
        <div className="grid gap-3 sm:grid-cols-2">
          {challenges.map((c) => (
            <div key={c.id} className="pl-card p-4">
              <div className="flex items-center justify-between"><p className="font-extrabold text-ink">{c.title}</p><Badge tone="warning">+{c.xpReward} XP</Badge></div>
              <p className="mt-1 text-body text-muted">{c.description}</p>
              <p className="mt-1 text-caption text-muted">{c.metric?.replace('_', ' ').toLowerCase()} goal: {c.targetValue}{c.endsOn ? ` · until ${c.endsOn}` : ''}</p>
              <Button size="sm" className="mt-3" disabled={(c.joined ?? []).length > 0 || pets.length === 0}
                onClick={() => joinChallenge(c.id!, pets[0].id).then((updated) => setChallenges((list) => list.map((x) => (x.id === updated.id ? updated : x)))).catch(() => {})}>
                {(c.joined ?? []).length > 0 ? 'Joined ✓' : 'Join challenge'}
              </Button>
            </div>
          ))}
          {challenges.length === 0 ? <p className="text-body text-muted">No active challenges this week.</p> : null}
        </div>
      </section>

      <section>
        <div className="mb-3 flex items-center justify-between">
          <h2 className="text-title">Leaderboard</h2>
          <div className="flex gap-1">
            <Chip selected={scope === 'neighborhood'} onClick={() => setScope('neighborhood')}>Neighborhood</Chip>
            <Chip selected={scope === 'global'} onClick={() => setScope('global')}>Global</Chip>
          </div>
        </div>
        <div className="pl-card divide-y divide-line">
          {(board?.entries ?? []).map((e) => (
            <Link key={e.petId} to={`/app/pets/${e.petId}`} className={`flex items-center gap-3 p-4 ${e.own ? 'bg-teal-light/60' : ''}`}>
              <span className="w-8 text-center text-title font-black text-teal-dark">{e.rank === 1 ? '🥇' : e.rank === 2 ? '🥈' : e.rank === 3 ? '🥉' : e.rank}</span>
              <Avatar name={e.petName ?? '?'} imageUrl={e.petAvatarUrl} size="sm" />
              <div className="min-w-0 flex-1">
                <p className="truncate font-extrabold text-ink">{e.petName}{e.own ? ' (you)' : ''}</p>
                <p className="text-caption text-muted">Lv. {e.level} {e.levelTitle}{e.neighborhood ? ` · ${e.neighborhood}` : ''}</p>
              </div>
              <p className="font-extrabold text-teal">{e.weeklyXp} XP</p>
            </Link>
          ))}
          {(board?.entries ?? []).length === 0 ? <p className="p-4 text-body text-muted">No leaderboard data yet — log an activity to climb the ranks!</p> : null}
        </div>
      </section>
    </div>
  );
}

function CommunityCard({ community: c }: { community: NonNullable<Discover['communities']>[number] }) {
  const [joined, setJoined] = useState(Boolean(c.joined));
  return (
    <div className="pl-card p-4">
      <Badge tone="accent">{c.kind?.toLowerCase()}</Badge>
      <p className="mt-2 font-extrabold text-ink">{c.name}</p>
      <p className="mt-1 line-clamp-2 text-body text-muted">{c.description}</p>
      <p className="mt-2 text-caption font-bold text-muted">{c.memberCount} members{c.upcomingEvents ? ` · ${c.upcomingEvents} upcoming events` : ''}</p>
      <Button size="sm" variant={joined ? 'outline' : 'secondary'} className="mt-3"
        onClick={() => joinCommunity(c.id!, !joined).then((r) => setJoined(Boolean(r.joined))).catch(() => {})}>
        {joined ? 'Joined ✓' : 'Join'}
      </Button>
    </div>
  );
}
