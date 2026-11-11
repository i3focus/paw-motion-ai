import { Navigate, Route, Routes } from 'react-router-dom';
import { PartnerAuthProvider, usePartnerAuth } from '../partnerAuth';
import { PartnerAuthPage } from './PartnerAuthPage';
import { PartnerBillingPage } from './PartnerBillingPage';
import { PartnerBookingsPage } from './PartnerBookingsPage';
import { PartnerCampaignDetailPage } from './PartnerCampaignDetailPage';
import { PartnerCampaignNewPage } from './PartnerCampaignNewPage';
import { PartnerCampaignsPage } from './PartnerCampaignsPage';
import { PartnerLayout } from './PartnerLayout';
import { PartnerProfilePage } from './PartnerProfilePage';
import { PartnerServicesPage } from './PartnerServicesPage';

function PartnerShell() {
  const { partner, loading } = usePartnerAuth();

  if (loading) {
    return <main className="flex min-h-screen items-center justify-center text-muted">Loading Partner Portal…</main>;
  }
  if (!partner) {
    return <PartnerAuthPage />;
  }

  return (
    <PartnerLayout>
      <Routes>
        <Route index element={<Navigate to="/partner/campaigns" replace />} />
        <Route path="campaigns" element={<PartnerCampaignsPage />} />
        <Route path="campaigns/new" element={<PartnerCampaignNewPage />} />
        <Route path="campaigns/:id" element={<PartnerCampaignDetailPage />} />
        <Route path="services" element={<PartnerServicesPage />} />
        <Route path="bookings" element={<PartnerBookingsPage />} />
        <Route path="billing" element={<PartnerBillingPage />} />
        <Route path="profile" element={<PartnerProfilePage />} />
        <Route path="*" element={<Navigate to="/partner/campaigns" replace />} />
      </Routes>
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
