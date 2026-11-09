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

## First recommended vertical slice

**Slice 1** because it proves identity, ownership, persistence, API contracts, and the core demo loop before adding AI or gamification complexity.
