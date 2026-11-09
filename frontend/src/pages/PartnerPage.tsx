import { Card, EmptyState } from '../components/ui';

export function PartnerPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-cream-900">Partner Portal</h1>
        <p className="text-cream-600">Campaign analytics are aggregate-only and exclude individual pet owner histories.</p>
      </div>
      <Card title="Coming in the next slice">
        <EmptyState title="Partner dashboard planned" description="Business profiles, campaigns, offers, and aggregate analytics will be added after engagement rules." />
      </Card>
    </div>
  );
}
