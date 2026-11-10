# Security Architecture

## Authentication

- Preferred target: OIDC through Keycloak or a managed provider.
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
- Rate limits on campaign interaction tracking to prevent click inflation.

## Data privacy

- LGPD-oriented minimization: collect only fields needed for the MVP.
- No personal data in logs; correlation IDs only.
- Business analytics are aggregate-only, with k-anonymity grouping for small breakdowns.
- GPS routes and owner locations are personal data: explicit OS permission, owner-only access, never shared with partners, opt-in Strava publication.
- Card data never reaches PawMotion servers (hosted checkout / tokenization) to minimize PCI scope.
- Account deletion and retention workflow must be implemented before production.

## Secrets

- JWT signing keys, DB credentials, AI API keys, payment provider keys/webhook secrets, Strava client secret, and the integration token encryption key come from environment/secret manager.
- Strava OAuth tokens are encrypted at rest (AES-GCM with `INTEGRATION_TOKEN_ENCRYPTION_KEY`).
- `.env.example` contains placeholders only.
- CI scans should prevent committed secrets.
