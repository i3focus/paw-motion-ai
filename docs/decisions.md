# Decisions log

Decisions that change scope, data model or cost during the PETLIFE refactor. Each entry records the simplest reversible option taken. Longer-lived architecture choices are promoted to ADRs under `docs/adr/`.

| ID | Date | Decision | Why | Reversal path |
|---|---|---|---|---|
| D-001 | 2026-11-11 | Product name in all UI, copy and docs becomes **PETLIFE** ("PetLife AI" for the assistant). Repository, Gradle project and Java package names (`com.pawmotionai`, `paw-motion-ai`) stay unchanged. | References use PETLIFE; renaming packages adds churn without user value. | Rename packages later in a mechanical PR. |
| D-002 | 2026-11-11 | New backend modules are written in **Kotlin** (Kotlin JVM + `kotlin-spring` + `kotlin-jpa` plugins); existing Java modules stay Java and are extended in Java. | Brief: keep language split; new modules in Kotlin unless the module is already Java. | Kotlin modules are self-contained per package. |
| D-003 | 2026-11-11 | OpenAPI is generated from code with **springdoc-openapi** and exported to `docs/api/openapi.yaml` by a Gradle task; TypeScript types are generated with **openapi-typescript** into `packages/api-client`. | Single source of truth; brief requires generated clients. | Generated files are committed; can switch generator. |
| D-004 | 2026-11-11 | Reaction migration mapping: `LIKE→PAW`, `LOVE→HEART`, `CHEER→LAUGH`; original value kept in `reactions.legacy_type`. | Typed reactions replace "like" with no data loss. | `legacy_type` allows re-mapping. |
| D-005 | 2026-11-11 | Campaign status model: `DRAFT`, `IN_REVIEW`, `ACTIVE`, `PAUSED`, `ENDED` (+ `end_reason` `ARCHIVED`/`EXPIRED`/`BUDGET_EXHAUSTED`/`REJECTED`/`PARTNER_ENDED`). Existing `ACTIVE` campaigns are grandfathered as approved. Credits are still debited at creation (budget = credits charged); existing refund accounting (`credits_refunded`) is preserved. | Preserves ledger semantics and history while adding review. | Status mapping is reversible via `end_reason`. |
| D-006 | 2026-11-11 | Shared code lives in `packages/{design-tokens,api-client,shared}` as plain TypeScript consumed from source through `file:` dependencies (no npm workspaces, no build step). Metro uses `watchFolders`. | Smallest change to existing per-app lockfiles and CI. | Can move to workspaces later. |
| D-007 | 2026-11-11 | Media uploads are stored through a `MediaStoragePort` with a local-disk adapter (Docker volume) and served from `/media/**`; S3-compatible adapter deferred. | No cloud storage provisioned. | Swap adapter via config. |
| D-008 | 2026-11-11 | Geospatial: enable PostGIS extension and use `geography(Point,4326)` + GIST indexes for places, lost-pet alerts, partner profiles and campaign targeting. Activity routes remain GeoJSON (ADR-0017). | Compose already runs `postgis/postgis`. | — |
| D-009 | 2026-11-11 | AI assistant uses a provider port with an OpenAI adapter (enabled when `OPENAI_API_KEY` is set) and a deterministic rule-based fallback used in local dev and CI. Actions (bookings, reminders, playdates) are stored as `PROPOSED` and executed only after `POST /confirm`. Sponsored partner suggestions carry `sponsored=true` and are labeled "Sponsored". | Keys/prompts server-side; brief requires confirm-before-execute. | Provider selectable by config. |
| D-010 | 2026-11-11 | Plans: `owners.plan` `FREE`/`PREMIUM`; FREE is limited to 10 AI messages per month; the multi-pet limit is **not enforced** (existing users already have several pets). Premium checkout for owners is **out of scope** of this refactor (landing CTA goes to sign-up). | Landing advertises plans; owner payments are a new billing flow. | Add owner subscriptions later using billing module. |
| D-011 | 2026-11-11 | Admin role: `owners.role` gains `ADMIN`; admins are created via env-configured bootstrap (`ADMIN_EMAILS`) on login. Campaign moderation and partner verification are admin-only. | Existing admin endpoints check a JWT claim; reuse it. | — |
| D-012 | 2026-11-11 | Web routing uses `react-router` with `/` (landing), `/signup`, `/login`, `/app/*` (owner), `/partner/*` (partner), `/admin/*` (moderation). | Landing must be public; deep links needed. | — |
| D-013 | 2026-11-11 | Typography: Nunito (rounded, 600–900 weights) bundled via `@fontsource/nunito` on web and `@expo-google-fonts/nunito` on mobile, falling back to `ui-rounded`. Body text ≥ 15px. | References specify `ui-rounded, 'SF Pro Rounded', 'Nunito'`; bundling ensures consistency on Android/Linux. | Token change only. |
| D-014 | 2026-11-11 | Push notifications use Expo's push HTTP API behind `PushGatewayPort`; tests use a stub gateway. | Brief requires Expo push. | — |
| D-015 | 2026-11-11 | Coral `#FF6B57` is the only action color; button text on coral is `#3A0F09` (coral-ink, from the reference) — passes WCAG AA; white on coral is 2.8:1 and is banned (5.98:1). Teal secondary buttons use white text on `#0F5C5C` (7.76:1). | Brief + reference CSS. | Token change only. |

## D-016 — Phase 2 schema deviations from the plan
- `activities.share_to_paw_city` keeps its name. The API exposes it as `sharedToFeed`; renaming the column would only churn the JPA mapping.
- `feed_items.type` stores `POST`/`ACTIVITY`/`ACHIEVEMENT` only. `SPONSORED` items come from campaigns and are interleaved when the feed is served, not persisted.
- There is no separate `review_status` column: `IN_REVIEW` is a campaign status, and a rejected campaign returns to `DRAFT` with `rejection_reason`.
- Levels and streaks are computed from `xp_events` and `activities` at read time. No read-model tables yet.
- The new Kotlin modules use `JdbcClient` (no JPA entities), so PostGIS `geography` columns and arrays need no Hibernate types.

## Refactor decisions (2026-11)

| ID | Date | Decision | Why | Reversal path |
|---|---|---|---|---|
| D-REF-01 | 2026-11-11 | `owners` identity-only; partner business data → `partner_profiles` | stop `/api/me` mutating partner fields; cleaner auth boundary (ADR-0021) | re-add columns via new migration |
| D-REF-02 | 2026-11-11 | Legacy social/owner columns kept through phases 2–5, dropped in V19 | zero-downtime cutover; `LegacyDataMigrationIT` proves loss-free (ADR-0023) | n/a — data was copied forward |
| D-REF-03 | 2026-11-11 | `POST /api/partner/campaigns` (v1 create) retained; analyze/archive/list removed | create path still used; removed paths were dead or bypassed moderation | restore endpoints if a caller appears |
| D-REF-04 | 2026-11-11 | Key-reuse mismatch returns 400 (not 422) | consistent with existing error handler | change mapper + client handling |
| D-REF-05 | 2026-11-11 | Social creates (posts/comments/reactions/bookings) ship without idempotency keys | duplicates are UX noise; keys reserved for credit-moving creates (ADR-0024) | add `Idempotency-Key` handling per endpoint |
| D-REF-06 | 2026-11-11 | Push tokens registered but delivery untested without EAS `projectId` | no Expo credentials in MVP env | run `eas init`, set `extra.eas.projectId` |
