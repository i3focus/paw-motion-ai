import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react';
import { NavLink, useLocation, useNavigate } from 'react-router-dom';
import { useAuth } from '../auth';
import { getUnreadCount } from '../api/petlife';
import { listPets } from '../api/client';
import type { Pet } from '../types';
import { Avatar, BottomTabBar, BrandLogo, type TabItem } from './ui';
import { CreatePostSheet } from './CreatePostSheet';

const icons = {
  feed: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><rect x="3" y="3" width="8" height="8" rx="2"/><rect x="13" y="3" width="8" height="8" rx="2"/><rect x="3" y="13" width="8" height="8" rx="2"/><rect x="13" y="13" width="8" height="8" rx="2"/></svg>,
  map: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 21s-7-5.3-7-11a7 7 0 1 1 14 0c0 5.7-7 11-7 11z"/><circle cx="12" cy="10" r="2.5"/></svg>,
  ai: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><path d="M12 3l1.8 5.2L19 10l-5.2 1.8L12 17l-1.8-5.2L5 10l5.2-1.8z"/></svg>,
  profile: <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4 3.6-6 8-6s8 2 8 6"/></svg>,
};

export type AppTab = 'feed' | 'map' | 'ai' | 'profile';
const tabs: [TabItem<AppTab>, TabItem<AppTab>, TabItem<AppTab>, TabItem<AppTab>] = [
  { key: 'feed', label: 'Feed', icon: icons.feed },
  { key: 'map', label: 'Map', icon: icons.map },
  { key: 'ai', label: 'AI', icon: icons.ai },
  { key: 'profile', label: 'Profile', icon: icons.profile },
];

export function AppShell({ children }: { children: ReactNode }) {
  const { owner, logout } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();
  const [sheetOpen, setSheetOpen] = useState(false);
  const [unread, setUnread] = useState(0);
  const [pets, setPets] = useState<Pet[]>([]);

  useEffect(() => {
    getUnreadCount().then((c) => setUnread(c.unread)).catch(() => setUnread(0));
  }, [location.pathname]);
  useEffect(() => { listPets().then(setPets).catch(() => setPets([])); }, []);

  const active: AppTab = useMemo(() => {
    const path = location.pathname;
    if (path.startsWith('/app/map')) return 'map';
    if (path.startsWith('/app/ai')) return 'ai';
    if (path.startsWith('/app/profile') || path.startsWith('/app/pets')) return 'profile';
    return 'feed';
  }, [location.pathname]);

  const onSelect = useCallback((key: AppTab) => navigate(`/app/${key}`), [navigate]);

  const desktopLink = 'inline-flex min-h-11 items-center rounded-pill px-4 text-label font-extrabold transition';
  const desktopActive = 'bg-teal text-white';
  const desktopIdle = 'text-teal hover:bg-teal-light';
  const linkClass = ({ isActive }: { isActive: boolean }) => `${desktopLink} ${isActive ? desktopActive : desktopIdle}`;

  return (
    <div className="flex min-h-screen flex-col bg-cream">
      <header className="sticky top-0 z-40 border-b border-line bg-cream/95 backdrop-blur">
        <div className="mx-auto flex h-16 w-full max-w-5xl items-center justify-between gap-3 px-4">
          <NavLink to="/app/feed" aria-label="PETLIFE feed" className="shrink-0"><BrandLogo /></NavLink>
          <nav aria-label="Main" className="hidden items-center gap-1 md:flex">
            <NavLink to="/app/feed" className={linkClass}>Feed</NavLink>
            <NavLink to="/app/map" className={linkClass}>Near me</NavLink>
            <NavLink to="/app/ai" className={linkClass}>PetLife AI</NavLink>
            <NavLink to="/app/discover" className={linkClass}>Discover</NavLink>
            {owner?.role === 'ADMIN' ? <NavLink to="/app/admin" className={linkClass}>Review</NavLink> : null}
          </nav>
          <div className="flex items-center gap-2">
            <NavLink to="/app/notifications" aria-label={`Notifications${unread ? `, ${unread} unread` : ''}`} className="relative grid h-11 w-11 place-items-center rounded-full bg-white text-teal shadow-card hover:bg-teal-light">
              <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="h-5 w-5"><path d="M18 9a6 6 0 1 0-12 0c0 6-2 6-2 6h16s-2 0-2-6"/><path d="M10 20a2 2 0 0 0 4 0"/></svg>
              {unread > 0 ? <span className="absolute -right-0.5 -top-0.5 grid h-5 min-w-5 place-items-center rounded-full bg-coral px-1 text-[11px] font-black text-coral-ink" aria-hidden="true">{unread > 9 ? '9+' : unread}</span> : null}
            </NavLink>
            <NavLink to="/app/profile" className="hidden md:block"><Avatar name={owner?.displayName ?? 'PetLife'} imageUrl={owner?.avatarUrl} size="sm" /></NavLink>
            <button type="button" onClick={() => { logout(); navigate('/'); }} className="hidden min-h-11 items-center rounded-pill px-4 text-label font-extrabold text-muted hover:bg-teal-light hover:text-teal md:inline-flex">Sign out</button>
          </div>
        </div>
      </header>

      <main className="mx-auto w-full max-w-5xl flex-1 px-4 pb-28 pt-6 md:pb-12">{children}</main>

      <div className="fixed inset-x-0 bottom-0 z-40 md:hidden">
        <BottomTabBar items={tabs} active={active} onSelect={onSelect} onCreate={() => setSheetOpen(true)} />
      </div>

      <button type="button" onClick={() => setSheetOpen(true)} className="fixed bottom-20 right-4 z-40 hidden min-h-12 items-center gap-2 rounded-pill bg-coral px-5 text-body font-black text-coral-ink shadow-coral transition hover:-translate-y-0.5 md:bottom-8 md:inline-flex">
        + New post
      </button>

      <CreatePostSheet open={sheetOpen} onClose={() => setSheetOpen(false)} pets={pets} />
    </div>
  );
}
