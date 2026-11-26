# Security Architecture

## Authentication

- Hackathon decision: the platform's own JWT issuer (ADR-0008). OIDC through Keycloak or a managed provider is a later option.
- MVP fallback: Spring Authorization Server or a development JWT issuer must be explicitly documented and disabled outside local/demo environments.
- Passwords are hashed with a modern adaptive algorithm; tokens are short-lived.

## Authorization

- Spring Security resource-server JWT validation.
- Roles: `OWNER`, `PARTNER`, `ADMIN`.
- Object-level checks ensure owners can access only their pets, activities, insights, goals, challenges, and coupons.
- Partners can access only campaigns they own and aggregate analytics.
- Partners can access only their own wallet, ledger entries, and payment transactions.
- Owners can access only their own routes, Strava connection, and publications.

## API security

- HTTPS in deployed environments.
- RFC 9457 Problem Details for errors without leaking internals.
- Bean Validation on all request DTOs.
- Rate limits on authentication, AI generation, and coupon redemption.
- Idempotency keys for coupon redemption, challenge completion, credit purchases, and campaign creation.
- Payment webhooks are public but require provider signature verification, event deduplication, and status re-fetch before crediting.
- Rate limits on campaign click tracking to prevent click inflation.

## Data privacy

- LGPD-oriented minimization: collect only fields needed for the MVP.
- No personal data in logs; correlation IDs only.
- Business analytics are aggregate-only click counters; no owner identity or attributes are exposed to partners.
- GPS routes and owner locations are personal data: explicit OS permission, owner-only access, never shared with partners, opt-in Strava publication.
- Card data never reaches PawMotion servers (hosted checkout / tokenization) to minimize PCI scope.
- Account deletion and retention workflow must be implemented before production.

## Secrets

- JWT signing keys, DB credentials, AI API keys, payment provider keys/webhook secrets, Strava client secret, and the integration token encryption key come from environment/secret manager.
- Strava OAuth tokens are encrypted at rest (AES-GCM with `INTEGRATION_TOKEN_ENCRYPTION_KEY`).
- `.env.example` contains placeholders only.
- CI scans should prevent committed secrets.
- AbacatePay webhooks: verify `X-Webhook-Secret` with constant-time comparison and the `X-Webhook-Signature` HMAC-SHA256 over the raw body; reject events whose `devMode` does not match the environment; re-fetch the checkout before granting credits; never log the webhook URL query secret. API keys are scoped to checkout permissions only.
- Optimistic (`@Version`) plus pessimistic (`SELECT … FOR UPDATE`) locking protect wallet balances against lost updates (ADR-0014).
- `ADMIN` role required for challenge/badge authoring, wallet unblock, and credit adjustments; every admin action is audited.

## As-built controls (post-refactor, 2026-11)

- JWT bearer auth (`JWT_SECRET`/`JWT_ISSUER` env); issued tokens carry a `roles`
  claim which the resource server maps to `ROLE_*` authorities. `ActiveAccountFilter`
  blocks suspended accounts; `@Secured` method security gates owner (`ROLE_OWNER`),
  partner (`ROLE_PARTNER`), and admin (`ROLE_ADMIN`) endpoints.
- `PATCH /api/me` is identity-only; partner business data requires the PARTNER role
  on `/api/partner/profile` (ADR-0021).
- Outbound URLs rendered as links (campaign CTA, partner website) pass a scheme
  allowlist (`safeHref` — http/https only; tested).
- Admin review mandatory before any sponsored content reaches feed/map (ADR-0022).
- `Idempotency-Key` on financial creates (ADR-0024); payment webhook verifies HMAC
  on the raw body.

## As-built controls (security audit remediation, 2026-11-25)

The `run-1` security audit (43 findings) was remediated across PRs #26–#59 and
closed with **30 fixed, 13 mitigated, 0 open** — see `docs/security/audit-closure.md`
for residual risk and `docs/security/remediation-summary.md` for the per-PR map.
Controls now in force:

**Edge / filter chain** (`SecurityConfiguration`, ordered around the bearer filter):

- `AuthRateLimitFilter` before authentication on login/register endpoints.
- `WriteRateLimitFilter` — per-account (`w:<account>`, 120/min) or per-IP for
  anonymous writes; dedicated `ai:<account>` bucket (30/min) for assistant/insights;
  dedicated `wh:<ip>` bucket for payment webhooks (starts in `shadow` mode, promote
  to `enforce` after observing `ratelimit.shadow_violations`).
- `ReadRateLimitFilter` — per-account read bucket (600/min) with a tighter class for
  costly reads (240/min).
- `ActiveAccountFilter` rejects suspended accounts after authentication.
- `RateLimitStore` is a fixed-window in-memory counter with a hard memory ceiling:
  unknown keys beyond `maxKeys` share one of 64 shard counters, so memory is bounded
  and sharing only makes limiting stricter.
- `ClientIpResolver` honors `X-Forwarded-For` only when the direct peer matches
  `TRUSTED_PROXIES` (exact `/32` of the backend nginx, `172.28.0.11/32` by default —
  never broad RFC1918 ranges). The same pattern applies per proxy hop; see
  `docs/architecture/edge-proxies.md`.

**JWT validation (N14)**: `JwtValidation` enforces issuer, audience and expiry.
Legacy issuers are accepted only inside a bounded compatibility window
(`JWT_LEGACY_COMPAT_UNTIL`, defaulting to one TTL after boot) so tokens minted
before rotation expire naturally; `JWT_SECRET` must be unique per environment.

**Fail-closed startup (C1/C2)**: `StartupSecurityValidator` is a dependency of the
JWT decoder/encoder beans and refuses boot when `JWT_SECRET` is blank, short
(< 32 bytes), a documented placeholder, or low-entropy — and, outside the `local`
profile, when the payment provider is not `abacatepay` with both API key and
webhook secret set.

**Authorization origin (C15)**: roles live in the database (`owners.role_source`,
V20). `ADMIN_EMAILS` bootstraps admins (`role_source=CONFIG`); reconciliation is
off by default (`ADMIN_RECONCILE=false`), so removing an e-mail does not revoke
the role until enabled.

**Quotas and bounds** (owner-locked `FOR NO KEY UPDATE`, `app.limits.mode`):
communities (5 total / 3 per day), community events (20 active / 10 per day),
activities (200/day); XP gain capped per day (`XP_DAILY_CAP=150`, `XP_MODE`);
media per owner (`MEDIA_MAX_BYTES_PER_OWNER`, `MEDIA_MAX_OBJECTS_PER_OWNER`,
atomic reservation in `owner_media_usage`, V25); AI usage capped at 10 free
messages/insights per month via atomic upsert in `ai_usage` (`AiQuota`, premium
unlimited); playdate invite notifications deduplicated per organizer/owner in a
24 h window with a daily cap (V22); lost-pet fanout cooldown per pet with a
per-owner daily cap (V23) and per-(alert, reporter) sighting caps (V24); campaign
ad events deduplicated per viewer in a 30-minute window (V28); activity
plausibility bounds enforced in the
application with a `NOT VALID` DB backstop (V27); one insight per owner/pet/day
for new rows (V26); campaign content frozen by moderation hash (V21).

**Actuator hardening (N4)**: only `/actuator/health` is public (no details);
`info`, `metrics` and everything else under `/actuator/**` requires ADMIN.

**Payments IO**: webhook secret compared in constant time, HMAC-SHA256 over the
raw body, `devMode` must match the environment, checkout re-fetched before
crediting, and webhook events are deduplicated (V28 index); outbound provider
calls have explicit connect/read timeouts.

**Post-commit side effects (N15)**: push and e-mail are sent after the commit
with bounded timeouts and retries (best effort; no durable outbox — residual
risk documented). Media files are deleted post-commit through the
`media_deletions` outbox table filled by a DB trigger, drained by a scheduled
job (`MediaDeletionOutbox`); orphan sweep is read-only unless
`MEDIA_ORPHAN_SWEEP_ENABLED=true`.

**Log hygiene**: e-mails are masked (`LogSafe.email`) and private free text is
kept out of logs.

**Client hardening**: mobile stores the access token in `expo-secure-store`
(Keychain/Keystore, `WHEN_UNLOCKED_THIS_DEVICE_ONLY`) with a one-time migration
from AsyncStorage, and Android backup is disabled (`allowBackup=false`);
frontend neutralizes rendered links without an http(s) scheme and host.

**Reverse-proxy chain**: see `docs/architecture/edge-proxies.md` — only the
gateway nginx publishes a port; backend nginx accepts `/api/` alone and the
backend trusts it alone.
