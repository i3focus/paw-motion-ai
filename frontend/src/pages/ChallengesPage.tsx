import { Card, EmptyState } from '../components/ui';

export function ChallengesPage() {
  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-slate-900">Challenges</h1>
        <p className="text-slate-600">Join challenges and track completion progress.</p>
      </div>
      <Card title="Coming in the next slice">
        <EmptyState title="Challenge engine planned" description="Backend contracts are documented; implementation is scheduled after the core activity slice." />
      </Card>
    </div>
  );
}
