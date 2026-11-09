import { useState } from 'react';
import { AuthProvider, useAuth } from './auth';
import { Layout, type PageKey } from './components/Layout';
import { AuthPage } from './pages/AuthPage';
import { DashboardPage } from './pages/DashboardPage';
import { PetsPage } from './pages/PetsPage';
import { ActivitiesPage } from './pages/ActivitiesPage';
import { InsightsPage } from './pages/InsightsPage';
import { ChallengesPage } from './pages/ChallengesPage';
import { RewardsPage } from './pages/RewardsPage';
import { PartnerPage } from './pages/PartnerPage';

function AppShell() {
  const { owner, loading } = useAuth();
  const [page, setPage] = useState<PageKey>('dashboard');

  if (loading) {
    return <main className="flex min-h-screen items-center justify-center text-slate-500">Loading PawMotion AI…</main>;
  }
  if (!owner) {
    return <AuthPage />;
  }

  return (
    <Layout page={page} setPage={setPage}>
      {page === 'dashboard' ? <DashboardPage /> : null}
      {page === 'pets' ? <PetsPage /> : null}
      {page === 'activities' ? <ActivitiesPage /> : null}
      {page === 'insights' ? <InsightsPage /> : null}
      {page === 'challenges' ? <ChallengesPage /> : null}
      {page === 'rewards' ? <RewardsPage /> : null}
      {page === 'partner' ? <PartnerPage /> : null}
    </Layout>
  );
}

export default function App() {
  return (
    <AuthProvider>
      <AppShell />
    </AuthProvider>
  );
}
