# PETLIFE refactor — final report

Outcome of the six-phase migration to the product model, flows, and visual system defined by `docs/design-reference/landing.html` and `docs/design-reference/app.html`. Phases shipped as sequential PRs (#13–#16 + phase 6) on `main`.

## What shipped per phase

| Phase | Scope | Result |
|---|---|---|
| 0 | Audit + `docs/refactor-plan.md` + `docs/decisions.md` | Plan reviewed |
| 1 | `packages/design-tokens`, web Tailwind theme + component library, RN theme/components | Shared tokens consumed by web + mobile |
| 2 | Backend: Kotlin modules (feed, map, assistant, discover, communities, bookings, notifications, gamification, ads v2, admin), V7–V18 migrations, OpenAPI export + `packages/api-client`, rate limits, structured logs | PR #13 merged |
| 3 | Web: public landing `/`, routed owner app `/app/*`, auth, partner shell kept | PR #14 merged |
| 4 | Mobile Expo rewrite on `expo-router` (tabs, modal create, deep links, push registration) | PR #15 merged |
| 5 | Partner portal (campaigns v2 lifecycle, services, bookings, billing, business profile), owner bookings + sponsored-card BOOKING CTA, admin review `/app/admin`, prorated campaign refunds | PR #16 merged |
| 6 | Deprecated removals (below), Jest + Vitest suites, docs, this report | This PR |

## Phase 6 — removals and cleanup

Removed once all callers/readers were gone:

- **Endpoints**: `/api/activities/{id}/reactions|comments`, `/api/activities/feed`, `/api/map/points`, `/api/promotions`, `/api/recommendations`, v1 campaign `GET /analyze` and `POST /archive`. The v1 `POST /api/partner/campaigns` remains the campaign create path (idempotent + ledger charge).
- **Code**: entire `social` Java package (controller, service, repository, ports, DTOs), owner promotions/recommendations stack, dead campaign use cases/services, dead client functions and types on web + mobile.
- **Schema (V19)**: `activity_reactions`, `activity_comments`, `owners.business_name/website/partner_description/latitude/longitude`. All data already lives in `reactions`, `comments`, and `partner_profiles` — verified by `LegacyDataMigrationIT` (V6 fixture → latest).
- **Partner business data**: registration now writes `partner_profiles` directly; `PATCH /api/me` is owner-identity only (displayName/bio/phone/avatarUrl). Partner profile edits go through `PATCH /api/partner/profile`.

## Tests and verification

- Backend: `./gradlew build` — 54+ Testcontainers-backed tests green on clean and legacy-fixture databases.
- Web: `npm run test` (Vitest, 10 tests — `safeHref` scheme gate, campaign draft validation/mapping), `tsc --noEmit`, `vite build`.
- Mobile: `npm run test` (Jest/jest-expo, 8 tests — API client 204/error semantics, validators), `tsc --noEmit`, `expo export` bundle verified.
- CI: GitHub Actions runs backend build+tests, frontend test+build, mobile typecheck+tests.
- `docs/api/openapi.yaml` and `packages/api-client/src/schema.d.ts` regenerated from the live app.

## Known limitations (deliberate)

- Mobile map pins are positional placeholders pending a native maps library (requires EAS dev builds; Expo Go cannot run them).
- Push delivery needs a real EAS `projectId` (`app.json` → `extra.eas.projectId` after `eas init`); tokens are registered server-side and the pipeline is wired, but device delivery requires production credentials.
- Partner "fake checkout" (`/partner/:purchaseId/billing/fake`) is a dev-mode payment simulator, intentionally retained until a real gateway is integrated.
- Campaign click events are not deduplicated server-side (impressions are); acceptable for v1 metrics.
- No browser-level e2e suite (Playwright) — API integration tests cover the backend flows; Vitest/Jest cover shared logic. Adding Playwright against a seeded backend is a follow-up.

## Data preservation

`LegacyDataMigrationIT` migrates a V6 fixture DB (owners, partners, pets, activities, legacy reactions/comments, campaigns in every status, wallets, payments) to latest and asserts: reactions migrated with `legacy_type` retained, comments preserved, campaign status/end_reason/budget mapping correct, `partner_profiles` backfilled from owner fields, places created for located partners, ledger untouched.
