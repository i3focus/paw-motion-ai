import type { ReactNode } from 'react';
import { Avatar, Badge, BrandLogo } from './ui';
import { useAuth } from '../auth';

export type PageKey = 'dashboard' | 'city' | 'pets' | 'activities' | 'insights' | 'profile';

const NAV_ITEMS: Array<{ key: PageKey; label: string }> = [
  { key: 'dashboard', label: 'Dashboard' },
  { key: 'city', label: 'Paw City' },
  { key: 'pets', label: 'Pets' },
  { key: 'activities', label: 'Activities' },
  { key: 'insights', label: 'Insights' },
  { key: 'profile', label: 'Profile' },
];

export function Layout({ page, setPage, children }: { page: PageKey; setPage: (page: PageKey) => void; children: ReactNode }) {
  const { owner, logout } = useAuth();

  return (
    <div className="min-h-screen bg-cream-50">
      <header className="sticky top-0 z-20 border-b border-cream-200 bg-white/90 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6">
          <div>
            <BrandLogo className="text-xl" />
            <p className="text-sm text-cream-500">Move together. Live better.</p>
          </div>
          <div className="flex items-center gap-3">
            <div className="hidden items-center gap-3 sm:flex">
              <Avatar name={owner?.displayName ?? 'Owner'} src={owner?.avatarUrl} className="h-10 w-10" />
              <div className="text-right">
                <p className="text-sm font-semibold text-cream-900">{owner?.displayName}</p>
                <p className="text-xs text-cream-500">{owner?.email}</p>
              </div>
            </div>
            <Badge tone="accent">Pet owner</Badge>
            <button type="button" onClick={logout} className="rounded-2xl border border-cream-300 px-3 py-2 text-sm font-semibold text-cream-700 hover:bg-cream-50">
              Logout
            </button>
          </div>
        </div>
        <nav className="mx-auto flex max-w-7xl gap-2 overflow-x-auto px-4 pb-3 sm:px-6">
          {NAV_ITEMS.map((item) => (
            <button
              key={item.key}
              type="button"
              onClick={() => setPage(item.key)}
              className={`whitespace-nowrap rounded-full px-4 py-2 text-sm font-semibold transition ${
                page === item.key ? 'bg-primary text-white shadow-sm' : 'bg-cream-100 text-cream-700 hover:bg-cream-200'
              }`}
            >
              {item.label}
            </button>
          ))}
        </nav>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6">{children}</main>
    </div>
  );
}
