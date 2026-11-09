import type { ReactNode } from 'react';
import type { Activity, Pet } from '../types';
import { Badge } from './ui';

export function PawIcon({ className = '' }: { className?: string }) {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true" className={className} fill="currentColor">
      <ellipse cx="12" cy="15.5" rx="4.8" ry="3.8" />
      <circle cx="6.2" cy="10.2" r="1.9" />
      <circle cx="10" cy="7.2" r="1.9" />
      <circle cx="14" cy="7.2" r="1.9" />
      <circle cx="17.8" cy="10.2" r="1.9" />
    </svg>
  );
}

export function PetAvatar({ pet, className = 'h-16 w-16' }: { pet: Pet; className?: string }) {
  if (pet.species === 'dog') {
    return (
      <span className={`relative inline-flex shrink-0 overflow-hidden rounded-[22px] border border-cream-200 bg-cream-100 ${className}`}>
        <img src="/assets/dog-portrait.jpg" alt={`${pet.name} avatar`} className="h-full w-full object-cover" />
        <PawIcon className="absolute bottom-1 right-1 h-5 w-5 text-white drop-shadow" />
      </span>
    );
  }

  return (
    <span className={`inline-flex shrink-0 items-center justify-center rounded-[22px] border border-cream-200 bg-gradient-to-br from-primary to-accent ${className}`}>
      <PawIcon className="h-7 w-7 text-white" />
    </span>
  );
}

export function PetCard({ pet, action }: { pet: Pet; action?: ReactNode }) {
  return (
    <article className="rounded-[28px] border border-cream-200 bg-white p-5 shadow-[0_24px_60px_-35px_rgba(54,39,20,0.4)]">
      <div className="flex items-start gap-4">
        <PetAvatar pet={pet} className="h-20 w-20" />
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center justify-between gap-2">
            <h3 className="truncate text-lg font-bold capitalize text-cream-900">{pet.name}</h3>
            <Badge tone="accent">{pet.status}</Badge>
          </div>
          <p className="mt-1 text-sm capitalize text-cream-600">{pet.species} · {pet.size}</p>
          {pet.breed ? <p className="mt-2 text-sm text-cream-700">{pet.breed}</p> : null}
          {pet.activityPreference ? <p className="mt-1 text-sm text-cream-500">Preference: {pet.activityPreference}</p> : null}
          {pet.birthDate ? <p className="mt-1 text-xs text-cream-500">Born {new Date(`${pet.birthDate}T00:00:00`).toLocaleDateString()}</p> : null}
        </div>
      </div>
      {action ? <div className="mt-4 flex flex-wrap gap-2">{action}</div> : null}
    </article>
  );
}

export function ActivityCard({ activity, petName, compact = false }: { activity: Activity; petName?: string; compact?: boolean }) {
  const minutes = Math.round(activity.durationSeconds / 60);
  const distance = activity.distanceMeters ? `${(activity.distanceMeters / 1000).toFixed(2)} km` : 'No distance';

  if (compact) {
    return (
      <article className="flex items-center gap-4 rounded-3xl border border-cream-200 bg-cream-50 p-4">
        <span className="inline-flex h-12 w-12 items-center justify-center rounded-2xl bg-accent-soft text-primary">
          <PawIcon className="h-6 w-6" />
        </span>
        <div className="min-w-0 flex-1">
          <div className="flex flex-wrap items-center gap-2">
            <p className="font-semibold capitalize text-cream-900">{activity.type.replace('_', ' ')}</p>
            {petName ? <Badge>{petName}</Badge> : null}
          </div>
          <p className="mt-1 text-sm text-cream-500">{new Date(activity.startedAt).toLocaleString()}</p>
          <p className="mt-1 text-sm font-medium text-cream-700">{minutes} min · {distance}</p>
        </div>
      </article>
    );
  }

  return (
    <article className="overflow-hidden rounded-[28px] border border-cream-200 bg-white shadow-[0_24px_60px_-35px_rgba(54,39,20,0.4)]">
      <div className="relative h-40 overflow-hidden sm:h-48">
        <img src="/assets/dog-walk.jpg" alt="Dog walking outdoors" className="h-full w-full object-cover" />
        <div className="absolute inset-0 bg-gradient-to-t from-dark/55 via-dark/10 to-transparent" />
        <div className="absolute left-4 top-4 flex items-center gap-2">
          <span className="inline-flex h-10 w-10 items-center justify-center rounded-2xl bg-white/90 text-primary shadow-sm">
            <PawIcon className="h-5 w-5" />
          </span>
          <Badge tone="accent">{activity.type.replace('_', ' ')}</Badge>
        </div>
        <div className="absolute bottom-4 left-4 right-4 text-white">
          <h3 className="font-display text-xl font-bold">{petName ? `${petName}'s activity` : 'Pet activity'}</h3>
          <p className="text-sm text-white/85">{new Date(activity.startedAt).toLocaleString()}</p>
        </div>
      </div>
      <div className="grid grid-cols-2 gap-3 p-5 sm:grid-cols-3">
        <div className="rounded-2xl bg-cream-50 p-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-cream-500">Duration</p>
          <p className="mt-1 text-2xl font-bold text-cream-900">{minutes}<span className="text-sm font-semibold text-cream-500"> min</span></p>
        </div>
        <div className="rounded-2xl bg-cream-50 p-3">
          <p className="text-xs font-semibold uppercase tracking-wide text-cream-500">Distance</p>
          <p className="mt-1 text-2xl font-bold text-cream-900">{activity.distanceMeters ? (activity.distanceMeters / 1000).toFixed(1) : '—'}<span className="text-sm font-semibold text-cream-500"> km</span></p>
        </div>
        <div className="rounded-2xl bg-cream-50 p-3 col-span-2 sm:col-span-1">
          <p className="text-xs font-semibold uppercase tracking-wide text-cream-500">Pet</p>
          <p className="mt-1 truncate text-2xl font-bold capitalize text-cream-900">{petName ?? 'Pet'}</p>
        </div>
      </div>
      {activity.notes ? <p className="px-5 pb-5 text-sm text-cream-600">{activity.notes}</p> : null}
    </article>
  );
}
