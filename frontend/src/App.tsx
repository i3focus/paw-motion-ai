import type { ReactNode } from 'react';
import { BrowserRouter, Navigate, Outlet, Route, Routes } from 'react-router-dom';
import { AuthProvider, useAuth } from './auth';
import { AppShell } from './components/AppShell';
import { LoadingState } from './components/ui';
import { LandingPage } from './pages/LandingPage';
import { AuthPage } from './pages/AuthPage';
import { FeedPage } from './pages/app/FeedPage';
import { MapPage } from './pages/app/MapPage';
import { AiPage } from './pages/app/AiPage';
import { PetProfilePage } from './pages/app/PetProfilePage';
import { ProfilePage } from './pages/app/ProfilePage';
import { DiscoverPage } from './pages/app/DiscoverPage';
import { NotificationsPage } from './pages/app/NotificationsPage';
import { PartnerFakeCheckoutPage } from './partner/PartnerFakeCheckoutPage';
import { PartnerPortal } from './partner/PartnerPortal';

function RequireOwnerAuth() {
  const { owner, loading } = useAuth();
  if (loading) {
    return <main className="flex min-h-screen items-center justify-center"><LoadingState label="Loading PETLIFE…" /></main>;
  }
  if (!owner) {
    return <Navigate to="/login" replace />;
  }
  if (owner.role === 'PARTNER') {
    return <Navigate to="/partner" replace />;
  }
  return (
    <AppShell>
      <Outlet />
    </AppShell>
  );
}

function RedirectIfAuthed({ children }: { children: ReactNode }) {
  const { owner, loading } = useAuth();
  if (loading) {
    return <main className="flex min-h-screen items-center justify-center"><LoadingState label="Loading PETLIFE…" /></main>;
  }
  if (owner) {
    return <Navigate to={owner.role === 'PARTNER' ? '/partner' : '/app/feed'} replace />;
  }
  return children;
}

export default function App() {
  return (
    <BrowserRouter>
      <Routes>
        <Route path="/partner/:purchaseId/billing/fake" element={<PartnerFakeCheckoutPage />} />
        <Route path="/partner/*" element={<PartnerPortal />} />
        <Route path="/" element={<AuthProvider><Outlet /></AuthProvider>}>
          <Route index element={<LandingPage />} />
          <Route path="login" element={<RedirectIfAuthed><AuthPage mode="login" /></RedirectIfAuthed>} />
          <Route path="signup" element={<RedirectIfAuthed><AuthPage mode="register" /></RedirectIfAuthed>} />
          <Route path="app" element={<RequireOwnerAuth />}>
            <Route index element={<Navigate to="/app/feed" replace />} />
            <Route path="feed" element={<FeedPage />} />
            <Route path="map" element={<MapPage />} />
            <Route path="ai" element={<AiPage />} />
            <Route path="pets/:petId" element={<PetProfilePage />} />
            <Route path="discover" element={<DiscoverPage />} />
            <Route path="notifications" element={<NotificationsPage />} />
            <Route path="profile" element={<ProfilePage />} />
            <Route path="*" element={<Navigate to="/app/feed" replace />} />
          </Route>
          <Route path="*" element={<Navigate to="/" replace />} />
        </Route>
      </Routes>
    </BrowserRouter>
  );
}
