import { useEffect, useState } from 'react';
import { ApiError, getDashboardSummary } from '../api/client';
import { Badge, Card, EmptyState, ErrorMessage, ListRow, SectionHeader, StatCard } from '../components/ui';
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
      <SectionHeader title="Dashboard" subtitle="Your pet activity overview for the last 7 days." />
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
          <Card title="Recent activities">
            {summary.recentActivities.length === 0 ? (
              <EmptyState title="No activities yet" description="Create a pet and record your first walk to see insights here." />
            ) : (
              <div className="space-y-3">
                {summary.recentActivities.map((activity) => (
                  <ListRow
                    key={activity.id}
                    leading={<Badge tone="accent">{activity.type.replace('_', ' ')}</Badge>}
                    title={new Date(activity.startedAt).toLocaleString()}
                    subtitle={`${Math.round(activity.durationSeconds / 60)} min${activity.distanceMeters ? ` · ${(activity.distanceMeters / 1000).toFixed(2)} km` : ''}`}
                  />
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
