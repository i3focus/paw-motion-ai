import { useState } from 'react';
import { AuthProvider, useAuth } from './auth';
import { Layout, type PageKey } from './components/Layout';
import { AuthPage } from './pages/AuthPage';
import { DashboardPage } from './pages/DashboardPage';
import { PetsPage } from './pages/PetsPage';
import { ActivitiesPage } from './pages/ActivitiesPage';
import { InsightsPage } from './pages/InsightsPage';
import { ProfilePage } from './pages/ProfilePage';
import { PartnerPortal } from './partner/PartnerPortal';

function AppShell() {
  const { owner, loading } = useAuth();
  const [page, setPage] = useState<PageKey>('dashboard');

  if (loading) {
    return <main className="flex min-h-screen items-center justify-center text-cream-500">Loading PawMotion AI…</main>;
  }
  if (!owner) {
    return <AuthPage />;
  }
  if (owner.role === 'PARTNER') {
    return <PartnerPortal />;
  }

  return (
    <Layout page={page} setPage={setPage}>
      {page === 'dashboard' ? <DashboardPage /> : null}
      {page === 'pets' ? <PetsPage /> : null}
      {page === 'activities' ? <ActivitiesPage /> : null}
      {page === 'insights' ? <InsightsPage /> : null}
      {page === 'profile' ? <ProfilePage /> : null}
    </Layout>
  );
}

export default function App() {
  const isPartnerPortal = window.location.pathname.startsWith('/partner');

  if (isPartnerPortal) {
    return <PartnerPortal />;
  }

  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  );
}
