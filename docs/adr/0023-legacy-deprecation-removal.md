# ADR 0023 — Deprecated-alias → backfill → drop migration policy

Status: accepted (2026-11-11, refactor phases 2–6)

## Context

The redesign replaced legacy social tables (`activity_reactions`, `activity_comments`)
and owner-side partner columns. Dropping them immediately would break old clients
mid-rollout and risk data loss.

## Decision

Three-step policy for every removal:

1. **Introduce canonical replacement** (e.g. `reactions`, `comments`,
   `partner_profiles`, typed `feed_items`) while legacy endpoints stay as thin
   aliases writing to canonical storage.
2. **Backfill** legacy rows into canonical tables via Flyway, preserving IDs,
   timestamps, and relationships (`LegacyDataMigrationIT` proves a V6 fixture
   migrates to latest loss-free).
3. **Drop** legacy endpoints/columns in a final migration (V19) only after grep shows
   zero callers/readers across backend, web, and mobile.

## Consequences

- `V19__drop_legacy_social_and_owner_business_columns.sql` removed
  `activity_reactions`, `activity_comments`, and five owner columns with verified
  zero data loss.
- The same policy applies to any future rename/removal: alias → backfill → drop.
