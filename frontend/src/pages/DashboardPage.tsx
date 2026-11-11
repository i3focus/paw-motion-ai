import { useEffect, useState } from 'react';
import { ApiError, getDashboardSummary } from '../api/client';
import { ActivityCard } from '../components/petVisuals';
import { Card, EmptyState, ErrorMessage, SectionHeader, StatCard } from '../components/ui';
import type { DashboardSummary } from '../types';

export function DashboardPage() {
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    getDashboardSummary()
      .then(setSummary)
      .catch((caught) => setError(caught instanceof ApiError ? caught.message : 'Unable to load dashboard'));
  }, []);

  return (
    <div className="space-y-6">
      <SectionHeader title="Progress" subtitle="Weekly mission stats for you and your pets." />
      <ErrorMessage message={error} />
      {summary ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            <StatCard label="Pets" value={summary.petCount} />
            <StatCard label="Activities" value={summary.activityCountLast7Days} />
            <StatCard label="Distance" value={`${(summary.totalDistanceMetersLast7Days / 1000).toFixed(1)} km`} />
            <StatCard label="Duration" value={`${Math.round(summary.totalDurationSecondsLast7Days / 60)} min`} />
            <StatCard label="Streak" value={`${summary.currentStreakDays} days`} hint="Consecutive active days" />
          </div>
          <Card title="Recent missions" description="Latest sessions across your pets.">
            {summary.recentActivities.length === 0 ? (
              <EmptyState title="No activities yet" description="Create a pet and record your first walk to see insights here." />
            ) : (
              <div className="grid gap-4 sm:grid-cols-2">
                {summary.recentActivities.map((activity) => (
                  <ActivityCard key={activity.id} activity={activity} compact />
                ))}
              </div>
            )}
          </Card>
        </>
      ) : !error ? (
        <p className="text-cream-500">Loading dashboard…</p>
      ) : null}
    </div>
  );
}
