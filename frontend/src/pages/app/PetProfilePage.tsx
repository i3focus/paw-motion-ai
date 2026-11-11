import { useEffect, useMemo, useState } from 'react';
import { Link, useParams } from 'react-router-dom';
import { createPlaydate, followPet, getPetAdventures, getPetBadges, getPetFriends, getPetPosts, getPetProfile, getHealthPassport, type Adventure, type BadgeDto, type HealthPassport, type PetFriend, type PetPost, type PetProfile } from '../../api/petlife';
import { listPets } from '../../api/client';
import type { Pet } from '../../types';
import { Avatar, Badge, Button, EmptyState, LoadingState, ProgressBar } from '../../components/ui';
import { PawIcon } from '../../components/petVisuals';

type Tab = 'posts' | 'adventures' | 'badges' | 'health' | 'friends';

export function PetProfilePage() {
  const { petId = '' } = useParams();
  const [profile, setProfile] = useState<PetProfile | null>(null);
  const [tab, setTab] = useState<Tab>('posts');
  const [posts, setPosts] = useState<PetPost[] | null>(null);
  const [adventures, setAdventures] = useState<Adventure[] | null>(null);
  const [badges, setBadges] = useState<BadgeDto[] | null>(null);
  const [health, setHealth] = useState<HealthPassport | null>(null);
  const [friends, setFriends] = useState<PetFriend[] | null>(null);
  const [following, setFollowing] = useState(false);
  const [invited, setInvited] = useState(false);
  const [myPets, setMyPets] = useState<Pet[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setProfile(null); setPosts(null); setAdventures(null); setBadges(null); setHealth(null); setFriends(null);
    getPetProfile(petId).then((p) => { setProfile(p); setFollowing(Boolean(p.following)); })
      .catch((e) => setError(e instanceof Error ? e.message : 'Could not load this profile'));
    listPets().then(setMyPets).catch(() => setMyPets([]));
  }, [petId]);

  useEffect(() => {
    if (tab === 'posts' && posts === null) getPetPosts(petId).then((r) => setPosts(r.items ?? [])).catch(() => setPosts([]));
    if (tab === 'adventures' && adventures === null) getPetAdventures(petId).then((r) => setAdventures(r.items ?? [])).catch(() => setAdventures([]));
    if (tab === 'badges' && badges === null) getPetBadges(petId).then(setBadges).catch(() => setBadges([]));
    if (tab === 'health' && health === null) getHealthPassport(petId).then(setHealth).catch(() => setHealth({ records: [], upcoming: [], weights: [] }));
    if (tab === 'friends' && friends === null) getPetFriends(petId).then((r) => setFriends(r.items ?? [])).catch(() => setFriends([]));
  }, [tab, petId, posts, adventures, badges, health, friends]);

  const pet = profile?.pet;
  const eng = profile?.engagement;
  const stats = profile?.stats;
  const weekMax = useMemo(() => Math.max(1, ...(profile?.weeklyActivity ?? []).map((w) => w.minutes ?? 0)), [profile]);

  if (error) return <EmptyState title="Profile unavailable" description={error} />;
  if (!profile) return <LoadingState label="Loading profile…" />;

  const TABS: { key: Tab; label: string }[] = [
    { key: 'posts', label: 'Posts' }, { key: 'adventures', label: 'Adventures' }, { key: 'badges', label: 'Badges' },
    { key: 'health', label: 'Health' }, { key: 'friends', label: 'Friends' },
  ];

  return (
    <div className="mx-auto max-w-3xl space-y-5">
      <section className="pl-card">
        <div className="relative h-36 bg-gradient-to-br from-teal via-teal-light to-mint">
          {pet?.coverUrl ? <img src={pet.coverUrl} alt="" className="h-full w-full object-cover" /> : null}
          <div className="absolute -bottom-10 left-5">
            <span className="grid h-24 w-24 place-items-center overflow-hidden rounded-card border-4 border-cream bg-teal text-white shadow-card">
              {pet?.avatarUrl ? <img src={pet.avatarUrl} alt="" className="h-full w-full object-cover" /> : <PawIcon className="h-10 w-10" />}
            </span>
          </div>
        </div>
        <div className="px-5 pb-5 pt-12">
          <div className="flex flex-wrap items-start justify-between gap-3">
            <div>
              <h1 className="text-heading">{pet?.name}</h1>
              <p className="text-body text-muted">{[pet?.species, pet?.breed, pet?.birthDate ? `${new Date().getFullYear() - new Date(pet.birthDate).getFullYear()} yrs` : null].filter(Boolean).join(' · ')}</p>
              {pet?.bio ? <p className="mt-2 max-w-lg text-body text-ink">{pet.bio}</p> : null}
            </div>
            {!profile.own ? (
              <div className="flex gap-2">
                <Button variant={following ? 'outline' : 'secondary'} onClick={() => followPet(petId, !following).then((s) => setFollowing(Boolean(s.following))).catch(() => {})}>
                  {following ? 'Following' : 'Follow'}
                </Button>
                <Button disabled={myPets.length === 0} onClick={() => !invited && myPets[0] && createPlaydate({ petId: myPets[0].id, mode: 'NOW', inviteePetIds: [petId] }).then(() => setInvited(true)).catch(() => {})}>
                  🐶 {invited ? 'Invited!' : 'Playdate'}
                </Button>
              </div>
            ) : null}
          </div>

          <div className="mt-4 grid grid-cols-3 gap-3 text-center">
            {[[`${stats?.followers ?? 0}`, 'Followers'], [`${stats?.adventures ?? 0}`, 'Adventures'], [`${stats?.moments ?? 0}`, 'Moments']].map(([v, l]) => (
              <div key={l} className="rounded-xl bg-sand p-3"><p className="text-title font-black text-teal-dark">{v}</p><p className="text-caption font-bold text-muted">{l}</p></div>
            ))}
          </div>

          {eng ? (
            <div className="mt-4 rounded-xl bg-sand p-4">
              <div className="flex items-center justify-between">
                <Badge tone="warning">⭐ Lv. {eng.level} {eng.levelTitle}</Badge>
                <span className="text-label font-bold text-muted">{eng.xpIntoLevel}/{eng.xpForNextLevel ?? '∞'} XP</span>
              </div>
              <div className="mt-3"><ProgressBar value={eng.xpIntoLevel ?? 0} max={eng.xpForNextLevel ?? 1} label={`Level ${eng.level} progress`} /></div>
              <div className="mt-2 flex gap-4 text-label font-bold text-teal">
                <span>🔥 {eng.streakDays ?? 0}-day streak</span><span>🏅 {eng.badgeCount ?? 0} badges</span><span>✨ {eng.xp ?? 0} XP total</span>
              </div>
            </div>
          ) : null}
        </div>
      </section>

      <nav aria-label="Profile sections" className="flex gap-1 overflow-x-auto rounded-pill bg-white p-1 shadow-card">
        {TABS.map((t) => (
          <button key={t.key} type="button" onClick={() => setTab(t.key)} aria-current={tab === t.key}
            className={`min-h-11 flex-1 whitespace-nowrap rounded-pill px-4 text-label font-extrabold transition ${tab === t.key ? 'bg-teal text-white' : 'text-teal hover:bg-teal-light'}`}>
            {t.label}
          </button>
        ))}
      </nav>

      {tab === 'posts' ? (
        posts === null ? <LoadingState /> : posts.length === 0 ? <EmptyState title="No posts yet" description="Share this pet's first moment!" /> : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-3">
            {posts.map((p) => (
              <div key={p.postId} className="pl-card aspect-square">
                {p.media?.[0] ? <img src={p.media[0]} alt={p.caption ?? 'Post'} className="h-full w-full object-cover" loading="lazy" /> : (
                  <div className="flex h-full items-center justify-center bg-teal-light p-3 text-center text-caption font-bold text-teal">{p.caption}</div>
                )}
              </div>
            ))}
          </div>
        )
      ) : null}

      {tab === 'adventures' ? (
        adventures === null ? <LoadingState /> : adventures.length === 0 ? <EmptyState title="No adventures yet" description="Activities logged for this pet will appear here." /> : (
          <div className="space-y-2">
            {adventures.map((a) => (
              <div key={a.activityId} className="pl-card flex items-center gap-4 p-4">
                <span className="grid h-11 w-11 place-items-center rounded-xl bg-teal-light text-teal"><PawIcon className="h-5 w-5" /></span>
                <div className="min-w-0 flex-1">
                  <p className="font-extrabold capitalize text-ink">{a.type?.replace('_', ' ')}</p>
                  <p className="text-caption text-muted">{a.startedAt ? new Date(a.startedAt).toLocaleString() : ''}</p>
                </div>
                <p className="text-label font-bold text-teal">{Math.round((a.durationSeconds ?? 0) / 60)} min{a.distanceMeters ? ` · ${(a.distanceMeters / 1000).toFixed(1)} km` : ''}</p>
              </div>
            ))}
          </div>
        )
      ) : null}

      {tab === 'badges' ? (
        badges === null ? <LoadingState /> : badges.length === 0 ? <EmptyState title="No badges yet" description="Keep exploring to earn the first medal!" /> : (
          <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {badges.map((b) => (
              <div key={b.code} className={`pl-card p-4 text-center ${b.awarded ? '' : 'opacity-50 grayscale'}`}>
                <span className="text-4xl" aria-hidden="true">{b.emoji ?? '🏅'}</span>
                <p className="mt-2 font-extrabold text-ink">{b.name}</p>
                <p className="text-caption text-muted">{b.awarded ? (b.awardedAt ? new Date(b.awardedAt).toLocaleDateString() : 'Earned') : b.description}</p>
              </div>
            ))}
          </div>
        )
      ) : null}

      {tab === 'health' ? (
        health === null ? <LoadingState /> : (
          <div className="space-y-4">
            <section className="pl-card p-5">
              <h2 className="text-title">Health passport</h2>
              <p className="mt-1 text-body text-muted">Vaccination card · {health.records?.filter((r) => r.status === 'DONE').length ?? 0} of {health.records?.length ?? 0} up to date</p>
              <div className="mt-3 space-y-2">
                {(health.records ?? []).map((r) => (
                  <div key={r.id} className="flex items-center gap-3 rounded-lg bg-sand p-3">
                    <Badge tone={r.status === 'DONE' ? 'success' : r.status === 'OVERDUE' ? 'danger' : 'warning'}>{r.status === 'DONE' ? '✓' : r.status === 'OVERDUE' ? '!' : '⏳'}</Badge>
                    <div className="min-w-0 flex-1">
                      <p className="font-extrabold text-ink">{r.name}</p>
                      <p className="text-caption text-muted">{r.type?.replace('_', ' ')}{r.administeredOn ? ` · given ${r.administeredOn}` : ''}{r.dueOn ? ` · due ${r.dueOn}` : ''}</p>
                    </div>
                  </div>
                ))}
                {(health.records ?? []).length === 0 ? <p className="text-body text-muted">No health records shared for this pet.</p> : null}
              </div>
            </section>
            {(health.upcoming ?? []).length > 0 ? (
              <section className="pl-card p-5">
                <h2 className="text-title">Upcoming</h2>
                <div className="mt-3 space-y-2">
                  {health.upcoming!.map((r) => (
                    <div key={r.id} className="flex items-center gap-3 rounded-lg bg-amber-soft p-3">
                      <span className="text-xl" aria-hidden="true">📅</span>
                      <div><p className="font-extrabold text-ink">{r.name}</p><p className="text-caption text-muted">{r.clinicName ? `${r.clinicName} · ` : ''}{r.dueOn ? new Date(`${r.dueOn}T00:00:00`).toLocaleDateString(undefined, { day: 'numeric', month: 'short' }) : ''}</p></div>
                    </div>
                  ))}
                </div>
              </section>
            ) : null}
          </div>
        )
      ) : null}

      {tab === 'friends' ? (
        friends === null ? <LoadingState /> : friends.length === 0 ? <EmptyState title="No friends yet" description="Pets become friends when they follow each other." /> : (
          <div className="grid gap-3 sm:grid-cols-2">
            {friends.map((f) => (
              <Link key={f.petId} to={`/app/pets/${f.petId}`} className="pl-card flex items-center gap-3 p-4">
                <Avatar name={f.name ?? '?'} imageUrl={f.avatarUrl} />
                <div className="min-w-0"><p className="truncate font-extrabold text-ink">{f.name}</p><p className="text-caption text-muted">{f.species}{f.breed ? ` · ${f.breed}` : ''} · {f.ownerName}</p></div>
              </Link>
            ))}
          </div>
        )
      ) : null}

      <section className="pl-card p-5">
        <h2 className="text-title">This week's activity</h2>
        <div className="mt-4 flex h-32 items-end gap-2" role="img" aria-label="Weekly activity chart">
          {(profile.weeklyActivity ?? []).map((w) => (
            <div key={w.day} className="flex flex-1 flex-col items-center gap-1">
              <div className="w-full rounded-t-md bg-teal" style={{ height: `${Math.max(4, ((w.minutes ?? 0) / weekMax) * 100)}%` }} />
              <span className="text-caption font-bold text-muted">{w.day}</span>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}
