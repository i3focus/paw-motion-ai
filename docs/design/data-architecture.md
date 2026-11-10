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
| Activity | `activities` (+ `source`), `activity_routes` (GeoJSON JSONB), `activity_statistics_projection` |
| AI Insights | `insights`, `insight_provider_attempts` |
| Engagement | `weekly_goals`, `streaks`, `badges`, `challenges`, `challenge_enrollments`, `challenge_progress`, `rewards` |
| Partner | `partner_campaigns` (+ `credits_charged`, `idempotency_key`), `partner_public_profiles` (categories, services, website, address, `geography(Point,4326)`), `campaign_interactions`, `campaign_daily_metrics`, `reward_offers`, `coupons`, `coupon_redemptions` |
| Billing | `payment_transactions`, `payment_webhook_events` |
| Ledger | `ledger_wallets`, `ledger_accounts`, `ledger_entries` |
| Integration | `strava_connections`, `strava_publications` |
| Notification | `notifications`, `notification_preferences` |

## MVP schema slice

The initial migration creates `owners`, `pets`, and `activities`. Later migrations add goals, challenges, coupons, insights, and spatial columns.

## CR-2026-11 schema outline

| Table | Key columns and constraints |
|---|---|
| `ledger_wallets` | `id`, `partner_id` UNIQUE, `status` (`ACTIVE`/`BLOCKED`), `blocked_reason`, timestamps, `version BIGINT NOT NULL DEFAULT 0` (JPA `@Version`) |
| `ledger_accounts` | `id`, `wallet_id`, `code` (`MAIN`, `BONUS`, `PARTNER`), `debit_priority` (1, 2, 3), `status` (`OPEN`/`CLOSED`), `balance BIGINT CHECK (balance >= 0)`, `version BIGINT NOT NULL DEFAULT 0` (JPA `@Version`, plus `SELECT … FOR UPDATE` on postings); UNIQUE (`wallet_id`, `code`) |
| `ledger_entries` | `id`, `account_id`, `direction` (`CREDIT`/`DEBIT`), `amount BIGINT CHECK (amount > 0)`, `balance_after BIGINT`, `reference_type` (`PAYMENT`, `CAMPAIGN`, `CAMPAIGN_REFUND`, `PAYMENT_REVERSAL`, `ADJUSTMENT`), `reference_id`, `description`, `correlation_id`, `occurred_at`; UNIQUE (`account_id`, `reference_type`, `reference_id`, `direction`); no UPDATE/DELETE |
| `payment_transactions` | `id`, `partner_id`, `amount NUMERIC(19,4)`, `currency`, `cpp_snapshot NUMERIC(19,6)`, `expected_credits BIGINT`, `target_account_id`, `method`, `provider`, `provider_checkout_id`, `provider_checkout_url`, `status`, `version` (`@Version`), `idempotency_key` UNIQUE per partner, `failure_reason`, timestamps, `version` |
| `payment_webhook_events` | `provider`, `provider_event_id` (PK pair), `received_at`, `payload_hash` — deduplication |
| `partner_public_profiles` | `partner_id` PK, `categories` text[], `services` JSONB, `website`, `phone`, `address`, `location geography(Point,4326)` GiST index, `service_radius_km` |
| `campaign_clicks` | `id`, `campaign_id`, `partner_id`, `owner_hash`, `source`, `placement`, `occurred_at`; index (`campaign_id`, `occurred_at`) |
| `campaign_daily_metrics` | PK (`campaign_id`, `day`), `clicks`, `unique_clicks` (per day only; period uniques use `COUNT(DISTINCT owner_hash)` on `campaign_clicks`) |
| `partner_campaigns` (changed) | + `credits_charged BIGINT`, `credits_refunded BIGINT`, `expires_at`, status + `EXPIRED`/`ARCHIVED`; existing `version` (`@Version`) |
| `challenges` / `badges` / `challenge_enrollments` / `badge_awards` | challenge: `created_by` (`ADMIN`/`AI`), `status` (`DRAFT`/`PUBLISHED`/`CLOSED`), metric, target, period, `badge_id`; enrollment UNIQUE (`challenge_id`, `owner_id`); award UNIQUE (`badge_id`, `owner_id`) |
| `activity_routes` | `activity_id` PK, `geojson JSONB` (`Feature<LineString>` with timestamps), `start_lat/lng`, `end_lat/lng`, `point_count` (PostGIS deferred) |
| `strava_connections` | `owner_id` PK, `athlete_id`, `access_token_enc`, `refresh_token_enc`, `expires_at`, `scopes`, `auto_publish`, `status` |
| `strava_publications` | `activity_id` PK, `status`, `strava_activity_id`, `attempts`, `last_error`, timestamps |

Migrations are added per slice (`V4__ledger_and_billing.sql`, `V5__partner_public_profile_and_interactions.sql`, `V6__activity_routes_and_strava.sql`); PostGIS extension must be enabled before `V5`.

## Retention and deletion

- Activity facts are retained while the account is active.
- Account deletion anonymizes/removes owner, pet, activity, insight, coupon, and notification data according to legal retention requirements.
- Audit logs are separated from domain data and have their own retention policy.
- Payment transactions and ledger entries are retained for fiscal/audit purposes after partner account deletion, with personal fields anonymized.
- Raw campaign interactions are retained 13 months; aggregates are kept.
- Activity routes and Strava tokens are deleted with the owner account; Strava disconnect deletes tokens immediately.

## Retention (recommended, pending legal review)

| Data | Retention |
|---|---|
| Deleted account (owner/partner) | Soft delete, 30-day grace, then hard delete/anonymization within 15 days |
| Application access logs (IP, timestamp) | 6 months (Marco Civil da Internet, art. 15) |
| `payment_transactions`, `ledger_entries` | 5 years, personal fields anonymized after account deletion |
| `campaign_clicks` raw | 13 months, then aggregates only |
| Backups | Rotated within 35 days |
