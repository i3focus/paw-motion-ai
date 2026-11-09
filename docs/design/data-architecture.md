# Data Architecture

## Principles

- PostgreSQL is the system of record.
- PostGIS stores optional spatial activity data when GPS routes are introduced.
- UUID primary keys are used for public identifiers.
- Each bounded context owns its tables; no cross-context foreign keys.
- Flyway migrations are versioned and immutable after release.
- Audit fields (`created_at`, `updated_at`) are standard; `version` supports optimistic locking where needed.

## Logical ownership

| Context | Tables |
|---|---|
| Identity | `owners`, `owner_credentials` or credential columns on `owners` |
| Pet | `pets` |
| Activity | `activities`, `activity_routes`, `activity_statistics_projection` |
| AI Insights | `insights`, `insight_provider_attempts` |
| Engagement | `weekly_goals`, `streaks`, `badges`, `challenges`, `challenge_enrollments`, `challenge_progress`, `rewards` |
| Partner | `business_profiles`, `campaigns`, `reward_offers`, `coupons`, `coupon_redemptions` |
| Notification | `notifications`, `notification_preferences` |

## MVP schema slice

The initial migration creates `owners`, `pets`, and `activities`. Later migrations add goals, challenges, coupons, insights, and spatial columns.

## Retention and deletion

- Activity facts are retained while the account is active.
- Account deletion anonymizes/removes owner, pet, activity, insight, coupon, and notification data according to legal retention requirements.
- Audit logs are separated from domain data and have their own retention policy.
