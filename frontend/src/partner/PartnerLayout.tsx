import { useEffect, useState, type ReactNode } from 'react';
import { Link, NavLink, useLocation } from 'react-router-dom';
import { getWallet } from '../api/client';
import { Avatar, Badge, BrandLogo } from '../components/ui';
import { usePartnerAuth } from '../partnerAuth';

const NAV_ITEMS: Array<{ to: string; label: string }> = [
  { to: '/partner/campaigns', label: 'Campaigns' },
  { to: '/partner/services', label: 'Services' },
  { to: '/partner/bookings', label: 'Bookings' },
  { to: '/partner/billing', label: 'Credits & Billing' },
  { to: '/partner/profile', label: 'Profile' },
];

export function PartnerLayout({ children }: { children: ReactNode }) {
  const { partner, logout } = usePartnerAuth();
  const location = useLocation();
  const [balance, setBalance] = useState<number | null>(null);

  useEffect(() => {
    if (partner) getWallet().then((w) => setBalance(w.totalBalance ?? 0)).catch(() => setBalance(null));
  }, [partner, location.pathname]);

  return (
    <div className="min-h-screen bg-cream">
      <header className="sticky top-0 z-20 border-b border-line bg-white/85 backdrop-blur">
        <div className="mx-auto flex max-w-7xl items-center justify-between px-4 py-4 sm:px-6">
          <div className="relative">
            <BrandLogo className="text-xl" />
            <p className="text-body text-muted">Partner Portal</p>
          </div>
          <div className="flex items-center gap-3">
            <div className="hidden items-center gap-3 sm:flex">
              <Avatar name={partner?.displayName ?? 'Partner'} src={partner?.avatarUrl} className="h-10 w-10" />
              <div className="text-right">
                <p className="text-body font-semibold text-ink">{partner?.displayName}</p>
                <p className="text-label text-muted">{partner?.email}</p>
              </div>
            </div>
            <Link to="/partner/billing" title="Credits & billing"
              className="flex min-h-10 items-center gap-1.5 rounded-pill bg-amber-soft px-4 text-label font-black text-amber-ink hover:opacity-90">
              ⚡ {balance === null ? '—' : balance.toLocaleString()} credits
            </Link>
            <Badge tone="accent">Partner</Badge>
            <button type="button" onClick={logout} className="rounded-2xl border border-line px-3 py-2 text-body font-semibold text-muted hover:bg-cream">
              Logout
            </button>
          </div>
        </div>
        <nav className="mx-auto flex max-w-7xl gap-2 overflow-x-auto px-4 pb-3 sm:px-6" aria-label="Partner sections">
          {NAV_ITEMS.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              className={({ isActive }) => `whitespace-nowrap rounded-full px-4 py-2 text-body font-semibold transition ${
                isActive ? 'bg-teal text-white shadow-card' : 'bg-white/70 text-muted hover:bg-teal-light hover:text-teal'
              }`}
            >
              {item.label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6">{children}</main>
    </div>
  );
}
