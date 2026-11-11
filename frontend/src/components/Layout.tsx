import type { ReactNode } from 'react';
import { Avatar, Badge, BrandLogo } from './ui';
import { PawIcon } from './petVisuals';
import { useAuth } from '../auth';

export type PageKey = 'dashboard' | 'city' | 'pets' | 'activities' | 'businesses' | 'insights' | 'profile';
type NavIconName = 'home' | 'progress' | 'ai' | 'pets' | 'activities' | 'businesses' | 'profile';

const NAV_ITEMS: Array<{ key: PageKey; label: string; icon: NavIconName }> = [
  { key: 'city', label: 'Home', icon: 'home' },
  { key: 'dashboard', label: 'Progress', icon: 'progress' },
  { key: 'insights', label: 'PetLife AI', icon: 'ai' },
  { key: 'pets', label: 'My Pets', icon: 'pets' },
  { key: 'activities', label: 'Activities', icon: 'activities' },
  { key: 'businesses', label: 'Businesses', icon: 'businesses' },
  { key: 'profile', label: 'Profile', icon: 'profile' },
];

function NavIcon({ name }: { name: NavIconName }) {
  if (name === 'pets') {
    return <PawIcon className="h-5 w-5" />;
  }
  const paths: Record<Exclude<NavIconName, 'pets'>, ReactNode> = {
    home: <path d="M3.5 10.5 12 3.5l8.5 7v10a1 1 0 0 1-1 1h-5v-6h-5v6h-5a1 1 0 0 1-1-1v-10Z" />,
    progress: <path d="M4 19V10M10 19V5M16 19v-7M22 19H2" />,
    ai: <path d="M12 3l1.7 5.1L19 9.8l-5.3 1.7L12 16.6l-1.7-5.1L5 9.8l5.3-1.7L12 3Z" />,
    activities: <path d="M3 12h4l2-6 4 12 2-6h6" />,
    businesses: <path d="M4 10h16v8H4v-8ZM9 10V6h6v4M4 14h16" />,
    profile: <path d="M12 12a4 4 0 1 0 0-8 4 4 0 0 0 0 8ZM4.5 20c1.2-3.8 4.2-5.4 7.5-5.4s6.3 1.6 7.5 5.4" />,
  };
  return (
    <svg viewBox="0 0 24 24" className="h-5 w-5" fill={name === 'ai' ? 'currentColor' : 'none'} stroke={name === 'ai' ? 'none' : 'currentColor'} strokeWidth={name === 'ai' ? undefined : 1.8} strokeLinecap="round" strokeLinejoin="round" aria-hidden="true">
      {paths[name]}
    </svg>
  );
}

export function Layout({ page, setPage, children }: { page: PageKey; setPage: (page: PageKey) => void; children: ReactNode }) {
  const { owner, logout } = useAuth();

  const navButton = (item: { key: PageKey; label: string; icon: NavIconName }) => (
    <button
      key={item.key}
      type="button"
      onClick={() => setPage(item.key)}
      className={`flex items-center gap-3 rounded-2xl px-4 py-3 text-left text-body font-semibold transition ${
        page === item.key
          ? 'bg-teal text-white shadow-card'
          : 'text-muted hover:bg-teal-light hover:text-teal'
      }`}
    >
      <NavIcon name={item.icon} />
      <span>{item.label}</span>
    </button>
  );

  return (
    <div className="bg-cream min-h-screen">
      <div className="lg:hidden">
        <header className="sticky top-0 z-30 border-b border-line bg-white/90 backdrop-blur">
          <div className="flex items-center justify-between px-4 py-3">
            <BrandLogo className="text-lg" />
            <button type="button" onClick={logout} className="rounded-2xl border border-line px-3 py-2 text-body font-semibold text-muted hover:bg-cream">
              Logout
            </button>
          </div>
          <nav className="flex gap-2 overflow-x-auto px-4 pb-3">{NAV_ITEMS.map(navButton)}</nav>
        </header>
      </div>

      <aside className="fixed inset-y-0 left-0 z-40 hidden w-72 flex-col border-r border-line bg-white/90 px-5 py-6 backdrop-blur lg:flex">
        <div className="relative">
          <BrandLogo className="text-xl" />
          <p className="mt-1 text-body text-muted">Move together. Live better.</p>
        </div>
        <nav className="mt-8 space-y-2">{NAV_ITEMS.map(navButton)}</nav>
        <button
          type="button"
          onClick={() => setPage('activities')}
          className="mt-5 inline-flex items-center justify-center gap-2 rounded-full bg-teal px-5 py-3 text-body font-bold text-white shadow-card transition hover:bg-teal-dark"
        >
          <span className="text-lg leading-none">+</span>
          Create activity
        </button>
        <div className="mt-auto rounded-3xl border border-line bg-cream p-4">
          <div className="flex items-center gap-3">
            <Avatar name={owner?.displayName ?? 'Owner'} src={owner?.avatarUrl} size="sm" />
            <div className="min-w-0">
              <p className="truncate text-body font-semibold text-ink">{owner?.displayName}</p>
              <p className="truncate text-label text-muted">{owner?.email}</p>
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between gap-2">
            <Badge tone="accent">Pet owner</Badge>
            <button type="button" onClick={logout} className="rounded-2xl border border-line px-3 py-2 text-label font-semibold text-muted hover:bg-white">
              Logout
            </button>
          </div>
        </div>
      </aside>

      <main className="px-4 py-6 sm:px-6 lg:pl-[320px] lg:pr-8">{children}</main>
    </div>
  );
}
