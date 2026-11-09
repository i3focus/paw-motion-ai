import { Card, EmptyState } from '../components/ui';

export function RewardsPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-cream-900">Rewards</h1>
        <p className="text-cream-600">Earn badges and redeem coupons from completed challenges.</p>
      </div>
      <Card title="Coming in the next slice">
        <EmptyState title="Rewards planned" description="Coupon inventory and redemption rules will be implemented with concurrency safeguards." />
      </Card>
    </div>
  );
}
