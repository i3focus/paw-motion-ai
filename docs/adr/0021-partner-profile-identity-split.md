# ADR 0021 — Partner business data split from `owners` into `partner_profiles`

Status: accepted (2026-11-11, refactor phases 2/6)

## Context

Pre-refactor, partner-facing business fields (business name, website, description,
latitude/longitude) lived as nullable columns on `owners`. That conflated identity
with business profile, meant `PATCH /api/me` wrote partner fields, and left owners
carrying columns that only partners use.

## Decision

`owners` holds identity only (email, password hash, display name, role, status, bio,
phone, avatar). All partner business data lives in `partner_profiles` (business name,
category, description, website, address, PostGIS location, hours, cover, verified
flag) under `/api/partner/profile`. Partner registration inserts a `partner_profiles`
row transactionally (business name falls back to display name). Legacy columns were
backfilled in V3/V7 and dropped in V19.

## Consequences

- `Owner`/`OwnerJpaEntity`/`OwnerResponseDto` carry no partner fields — smaller
  identity surface, clearer authorization boundary.
- `/api/me` can no longer mutate business data; partners must use the role-gated
  partner endpoints.
- Verified-partner state lives with the profile, read by admin review.
