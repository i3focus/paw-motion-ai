# Implementation Plan

## Delivery strategy

Deliver vertical slices, not isolated layers. Each slice must include domain rules, persistence, API contract, frontend screen, tests, and documentation updates.

## Slice 0 — Foundation

- Repository structure: `backend`, `frontend`, `docs`.
- Spring Boot modular monolith skeleton with hexagonal package conventions.
- React/Vite/Tailwind frontend shell.
- Docker Compose, env examples, CI.

## Slice 1 — Register → Pet → Activity → Dashboard

1. Owner registration/authentication.
2. Pet profile CRUD scoped by owner.
3. Activity recording and owner-scoped history.
4. Dashboard summary statistics.
5. Frontend auth, pet, activity, dashboard screens.

## Slice 2 — AI Insights

1. Activity aggregate read model.
2. AI provider port, deterministic local provider, optional OpenAI/Gemini adapter.
3. Prompt templates, validation, fallback, metadata persistence.
4. Frontend insight card.

## Slice 3 — Engagement

1. Weekly goals, streaks, badges.
2. Challenges and enrollments.
3. Rewards and notifications.

## Slice 4 — Partner and Coupons

1. Business profile and campaign management.
2. Aggregate analytics dashboard.
3. Coupons, inventory, redemption, concurrency tests.

## Slice 5 — Hardening

1. Testcontainers integration tests.
2. OpenAPI contract tests.
3. Playwright critical-path E2E.
4. Security review, rate limits, account deletion.
5. Deployment documentation and seed data.

## Slice 6 — Partner monetization with credits (CR-2026-11)

1. Pricing configuration (CPP, campaign price, campaign credit cost) — T-016.
2. Ledger context: wallet, accounts, atomic postings — T-017.
3. Billing context: payment transactions, fake provider, purchase API — T-018.
4. Campaign creation debits credits — T-020.
5. Payment webhook/reconciliation and real provider sandbox — T-019.
6. Partner portal wallet UI — T-021.

## Slice 7 — Partner value and owner discovery

1. Campaign interaction tracking in owner apps — T-022.
2. Partner performance dashboard — T-023.
3. Partner public profile with location — T-024.
4. Owner partner detail page — T-025.
5. Partner map and campaign list — T-026.

## Slice 8 — Live tracking and Strava

1. Activity routes in PostGIS — T-027.
2. Mobile Home quick-start live tracking — T-028.
3. Outbox publisher — T-029.
4. Strava connection and publication — T-030, T-031.

Slices 6 and 8 are independent and can run in parallel (backend ledger/billing vs. mobile tracking). Slice 7 depends on campaign creation being stable. Every slice updates OpenAPI, `.env.example`, README, and tests (T-032, T-033).

## First recommended vertical slice

**Slice 1** because it proves identity, ownership, persistence, API contracts, and the core demo loop before adding AI or gamification complexity.

## Next recommended slice (CR-2026-11)

**Slice 6 (T-016 → T-017 → T-018 → T-020)** using the fake payment provider: it proves the ledger invariants (atomic, idempotent, non-negative) and the "campaign costs credits" rule end-to-end before a real payment provider is chosen. In parallel, T-027/T-028 deliver the mobile quick-start tracking.
