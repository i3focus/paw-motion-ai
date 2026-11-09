import { useEffect, useState } from 'react';
import { ApiError, getDashboardSummary } from '../api/client';
import { Card, EmptyState, ErrorMessage, StatCard } from '../components/ui';
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
      <div>
        <h1 className="text-2xl font-bold text-cream-900">Dashboard</h1>
        <p className="text-cream-600">Your pet activity overview for the last 7 days.</p>
      </div>
      <ErrorMessage message={error} />
      {summary ? (
        <>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-5">
            <StatCard label="Pets" value={summary.petCount} />
            <StatCard label="Activities" value={summary.activityCountLast7Days} />
            <StatCard label="Distance" value={`${(summary.totalDistanceMetersLast7Days / 1000).toFixed(1)} km`} />
            <StatCard label="Duration" value={`${Math.round(summary.totalDurationSecondsLast7Days / 60)} min`} />
            <StatCard label="Streak" value={`${summary.currentStreakDays} days`} />
          </div>
          <Card title="Recent activities">
            {summary.recentActivities.length === 0 ? (
              <EmptyState title="No activities yet" description="Create a pet and record your first walk to see insights here." />
            ) : (
              <div className="space-y-3">
                {summary.recentActivities.map((activity) => (
                  <div key={activity.id} className="flex items-center justify-between rounded-xl bg-cream-50 px-4 py-3">
                    <div>
                      <p className="font-medium capitalize text-cream-900">{activity.type.replace('_', ' ')}</p>
                      <p className="text-sm text-cream-500">{new Date(activity.startedAt).toLocaleString()}</p>
                    </div>
                    <div className="text-right text-sm text-cream-600">
                      <p>{Math.round(activity.durationSeconds / 60)} min</p>
                      <p>{activity.distanceMeters ? `${(activity.distanceMeters / 1000).toFixed(2)} km` : 'No distance'}</p>
                    </div>
                  </div>
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
