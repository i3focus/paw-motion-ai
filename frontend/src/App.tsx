import { useState } from 'react';
import { AuthProvider, useAuth } from './auth';
import { Layout, type PageKey } from './components/Layout';
import { AuthPage } from './pages/AuthPage';
import { CityPage } from './pages/CityPage';
import { DashboardPage } from './pages/DashboardPage';
import { PetsPage } from './pages/PetsPage';
import { ActivitiesPage } from './pages/ActivitiesPage';
import { BusinessesPage } from './pages/BusinessesPage';
import { InsightsPage } from './pages/InsightsPage';
import { ProfilePage } from './pages/ProfilePage';
import { PartnerFakeCheckoutPage } from './partner/PartnerFakeCheckoutPage';
import { PartnerPortal } from './partner/PartnerPortal';

function AppShell() {
  const { owner, loading } = useAuth();
  const [page, setPage] = useState<PageKey>('city');

  if (loading) {
    return <main className="flex min-h-screen items-center justify-center text-muted">Loading PETLIFE…</main>;
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
      {page === 'city' ? <CityPage /> : null}
      {page === 'pets' ? <PetsPage /> : null}
      {page === 'activities' ? <ActivitiesPage /> : null}
      {page === 'businesses' ? <BusinessesPage /> : null}
      {page === 'insights' ? <InsightsPage /> : null}
      {page === 'profile' ? <ProfilePage /> : null}
    </Layout>
  );
}

export default function App() {
  const isFakeCheckout = window.location.pathname.startsWith('/partner') && window.location.pathname.includes('/billing/fake');
  const isPartnerPortal = window.location.pathname.startsWith('/partner');

  if (isFakeCheckout) {
    return <PartnerFakeCheckoutPage />;
  }
  if (isPartnerPortal) {
    return <PartnerPortal />;
  }

  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  );
}
