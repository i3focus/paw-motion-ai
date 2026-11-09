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

## API security

- HTTPS in deployed environments.
- RFC 9457 Problem Details for errors without leaking internals.
- Bean Validation on all request DTOs.
- Rate limits on authentication, AI generation, and coupon redemption.
- Idempotency keys for coupon redemption and challenge completion.

## Data privacy

- LGPD-oriented minimization: collect only fields needed for the MVP.
- No personal data in logs; correlation IDs only.
- Business analytics are aggregate-only.
- Account deletion and retention workflow must be implemented before production.

## Secrets

- JWT signing keys, DB credentials, and AI API keys come from environment/secret manager.
- `.env.example` contains placeholders only.
- CI scans should prevent committed secrets.
