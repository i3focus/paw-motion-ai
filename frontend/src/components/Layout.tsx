import type { ReactNode } from 'react';
import { Avatar, Badge, BrandLogo, PoppyMark } from './ui';
import { useAuth } from '../auth';

export type PageKey = 'dashboard' | 'city' | 'pets' | 'activities' | 'businesses' | 'insights' | 'profile';

const NAV_ITEMS: Array<{ key: PageKey; label: string }> = [
  { key: 'city', label: 'Home' },
  { key: 'dashboard', label: 'Progress' },
  { key: 'insights', label: 'PetLife AI' },
  { key: 'pets', label: 'My Pets' },
  { key: 'activities', label: 'Activities' },
  { key: 'businesses', label: 'Businesses' },
  { key: 'profile', label: 'Profile' },
];

export function Layout({ page, setPage, children }: { page: PageKey; setPage: (page: PageKey) => void; children: ReactNode }) {
  const { owner, logout } = useAuth();

  const navButton = (item: { key: PageKey; label: string }) => (
    <button
      key={item.key}
      type="button"
      onClick={() => setPage(item.key)}
      className={`rounded-2xl px-4 py-3 text-left text-sm font-semibold transition ${
        page === item.key
          ? 'bg-primary text-white shadow-[0_16px_36px_-20px_rgba(130,94,49,1)]'
          : 'text-cream-700 hover:bg-accent-soft hover:text-primary'
      }`}
    >
      {item.label}
    </button>
  );

  return (
    <div className="paw-city-bg min-h-screen">
      <div className="lg:hidden">
        <header className="sticky top-0 z-30 border-b border-cream-200 bg-white/90 backdrop-blur">
          <div className="flex items-center justify-between px-4 py-3">
            <BrandLogo className="text-lg" />
            <button type="button" onClick={logout} className="rounded-2xl border border-cream-300 px-3 py-2 text-sm font-semibold text-cream-700 hover:bg-cream-50">
              Logout
            </button>
          </div>
          <nav className="flex gap-2 overflow-x-auto px-4 pb-3">{NAV_ITEMS.map(navButton)}</nav>
        </header>
      </div>

      <aside className="fixed inset-y-0 left-0 z-40 hidden w-72 flex-col border-r border-cream-200 bg-white/90 px-5 py-6 backdrop-blur lg:flex">
        <div className="relative">
          <BrandLogo className="text-xl" />
          <PoppyMark className="poppy-float pointer-events-none absolute -right-8 -top-3 h-9 w-9 text-accent/50" />
          <p className="mt-1 text-sm text-cream-500">Move together. Live better.</p>
        </div>
        <nav className="mt-8 space-y-2">{NAV_ITEMS.map(navButton)}</nav>
        <div className="mt-auto rounded-3xl border border-cream-200 bg-cream-50 p-4">
          <div className="flex items-center gap-3">
            <Avatar name={owner?.displayName ?? 'Owner'} src={owner?.avatarUrl} size="sm" />
            <div className="min-w-0">
              <p className="truncate text-sm font-semibold text-cream-900">{owner?.displayName}</p>
              <p className="truncate text-xs text-cream-500">{owner?.email}</p>
            </div>
          </div>
          <div className="mt-3 flex items-center justify-between gap-2">
            <Badge tone="accent">Pet owner</Badge>
            <button type="button" onClick={logout} className="rounded-2xl border border-cream-300 px-3 py-2 text-xs font-semibold text-cream-700 hover:bg-white">
              Logout
            </button>
          </div>
        </div>
      </aside>

      <main className="px-4 py-6 sm:px-6 lg:pl-[320px] lg:pr-8">{children}</main>
    </div>
  );
}
