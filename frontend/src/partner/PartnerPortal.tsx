import { useState } from 'react';
import { PartnerAuthProvider, usePartnerAuth } from '../partnerAuth';
import { PartnerAuthPage } from './PartnerAuthPage';
import { PartnerBillingPage } from './PartnerBillingPage';
import { PartnerCampaignsPage } from './PartnerCampaignsPage';
import { PartnerLayout, type PartnerPageKey } from './PartnerLayout';
import { PartnerProfilePage } from './PartnerProfilePage';

function PartnerShell() {
  const { partner, loading } = usePartnerAuth();
  const [page, setPage] = useState<PartnerPageKey>('campaigns');

  if (loading) {
    return <main className="flex min-h-screen items-center justify-center text-muted">Loading Partner Portal…</main>;
  }
  if (!partner) {
    return <PartnerAuthPage />;
  }

  return (
    <PartnerLayout page={page} setPage={setPage}>
      {page === 'campaigns' ? <PartnerCampaignsPage /> : null}
      {page === 'billing' ? <PartnerBillingPage /> : null}
      {page === 'profile' ? <PartnerProfilePage /> : null}
    </PartnerLayout>
  );
}

export function PartnerPortal() {
  return (
    <PartnerAuthProvider>
      <PartnerShell />
    </PartnerAuthProvider>
  );
}
