import type { ReactNode } from 'react';
import { useAuth } from '../auth';

export type PageKey = 'dashboard' | 'pets' | 'activities' | 'insights' | 'challenges' | 'rewards' | 'partner';

const NAV_ITEMS: Array<{ key: PageKey; label: string }> = [
  { key: 'dashboard', label: 'Dashboard' },
  { key: 'pets', label: 'Pets' },
  { key: 'activities', label: 'Activities' },
  { key: 'insights', label: 'AI Insights' },
  { key: 'challenges', label: 'Challenges' },
  { key: 'rewards', label: 'Rewards' },
  { key: 'partner', label: 'Partner Portal' },
];

export function Layout({ page, setPage, children }: { page: PageKey; setPage: (page: PageKey) => void; children: ReactNode }) {
  const { owner, logout } = useAuth();

  return (
    <div className="min-h-screen">
      <header className="border-b border-slate-200 bg-white">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6">
          <div>
            <p className="text-xl font-bold text-emerald-700">PawMotion AI</p>
            <p className="text-sm text-slate-500">Every step counts</p>
          </div>
          <div className="flex items-center gap-4">
            <div className="text-right">
              <p className="text-sm font-medium text-slate-900">{owner?.displayName}</p>
              <p className="text-xs text-slate-500">{owner?.email}</p>
            </div>
            <button type="button" onClick={logout} className="rounded-lg border border-slate-300 px-3 py-2 text-sm font-medium text-slate-700 hover:bg-slate-50">
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
              className={`whitespace-nowrap rounded-full px-4 py-2 text-sm font-medium transition ${
                page === item.key ? 'bg-emerald-600 text-white' : 'bg-slate-100 text-slate-700 hover:bg-slate-200'
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
