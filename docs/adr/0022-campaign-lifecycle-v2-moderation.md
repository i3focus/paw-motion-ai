# ADR 0022 — Campaign v2 lifecycle with mandatory admin review

Status: accepted (2026-11-11, refactor phase 5)

## Context

The original campaign flow let partners create ads that could reach the feed without
moderation (the `analyze` endpoint even self-activated campaigns). Sponsored content
needs review before publication; campaigns also needed real lifecycle states,
targeting, formats, and billing integration.

## Decision

Campaigns follow `DRAFT → IN_REVIEW → ACTIVE ⇄ PAUSED → ENDED` (+ `REJECTED`).
Only `DRAFT`/`REJECTED` are editable. Submission requires title, description, media,
CTA wiring, an active service (when `ctaType=BOOKING`), and a non-expired window —
all under a `FOR UPDATE` transition lock. Admins review at `/api/admin/ads/review`
with creative inspection (media, CTA destination, bound service). Ending a campaign
posts a real prorated `CAMPAIGN_REFUND` credit to the partner wallet.

## Consequences

- No path publishes a sponsored card without an admin approval row.
- Refunds are ledger-verifiable, not just status flips.
- Resubmit after rejection resets `submitted_at` intentionally.
