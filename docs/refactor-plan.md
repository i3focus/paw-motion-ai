# PETLIFE refactor plan

Status: **Complete** — all phases 0–6 shipped (PRs #13–#17 merged 2026-11-11). Execution record in `docs/refactor-report.md`; validation in `docs/validation-skills.md`.
Source of truth for design: `docs/design-reference/landing.html` and `docs/design-reference/app.html` (Brazilian Portuguese copy, translated to English during implementation). When existing code conflicts with the references, the references win. Scope, data-model and cost decisions are logged in [`docs/decisions.md`](decisions.md).

## 1. Current inventory (audit of `main` @ `dd6788e`)

### 1.1 Repository layout

| Path | What it is |
|---|---|
| `backend/` | Spring Boot 4.1 / Java 21 modular monolith (Gradle Kotlin DSL, 234 Java files, no Kotlin yet). Hexagonal layout per bounded context: `domain` → `application` (ports, use cases, DTOs) → `adapter` (REST, persistence, external). |
| `frontend/` | React 19 + Vite 8 + Tailwind 4 + TypeScript. Owner web app (state-based page switch, no router) and partner portal under `/partner`. |
| `mobile/` | Expo SDK 58 / React Native 0.88, owner-only app with a hand-rolled text tab bar. |
| `docs/` | SDD: requirements, ADR-0001..0018, DDD design, OpenAPI (hand-written, 841 lines), event schemas, backlog. |
| `docker-compose.yml` | `postgis/postgis:17-3.5`, backend, Nginx-served frontend. |
| `.github/workflows/ci.yml` | Backend `gradle build`, frontend `npm ci && npm run build`, mobile `npm ci && npm run typecheck`. |

### 1.2 Backend bounded contexts and REST surface

| Context | Packages | Endpoints | Persistence |
|---|---|---|---|
| identity | `identity.*` | `POST /api/auth/{register,login}`, `POST /api/partner/auth/{register,login}`, `GET/PATCH /api/me` | `owners` (role `OWNER`/`PARTNER`, profile + partner business fields, lat/lng) — JPA |
| pet | `pet.*` | `GET/POST /api/pets`, `GET/PATCH /api/pets/{id}` | `pets` — JPA |
| activity | `activity.*` | `GET/POST /api/activities`, `GET /api/dashboard/summary` | `activities` (+ lat/lng, `share_to_paw_city`) — JPA |
| aiinsights | `aiinsights.*` | `POST /api/insights` | `insights`; provider port `AiInsightProviderPort` with only `LocalRuleBasedInsightProvider` |
| social | `social.*` | `GET /api/feed`, `POST /api/activities/{id}/reactions`, `GET/POST /api/activities/{id}/comments`, `GET /api/map/points` | `activity_reactions` (`LIKE`/`LOVE`/`CHEER`), `activity_comments` — JdbcTemplate |
| partner | `partner.*` | `GET/POST /api/partner/campaigns`, `POST …/{id}/analyze`, `POST …/{id}/archive`, `GET /api/promotions`, `GET /api/recommendations` | `partner_campaigns` (status `DRAFT/ACTIVE/PAUSED/ARCHIVED/EXPIRED`, credits charged/refunded, expires_at) — JPA |
| ledger | `ledger.*` | `GET /api/partner/wallet`, `GET …/accounts/{id}/entries`, admin unblock/adjustments | `ledger_wallets`, `ledger_accounts`, `ledger_entries` (pessimistic + optimistic locking, ADR-0014) |
| billing | `billing.*` | `/api/partner/billing/{pricing,purchases}`, `POST /api/webhooks/payments/{provider}` | `payment_transactions`, `payment_webhook_events`; fake + AbacatePay gateways |
| configuration | — | security (stateless JWT resource server, HMAC), CORS, `AuthRateLimitFilter`, `ActiveAccountFilter`, `ApiExceptionHandler` | — |

Flyway migrations: `V1` identity/pet/activity, `V2` insights, `V3` profiles + campaigns, `V4` ledger/billing, `V5` social feed + map, `V6` payment customer id. Hibernate `ddl-auto: validate`.

Tests (7 classes, all unit-level, no Testcontainers): activity statistics, insight validator, auth rate limit filter, owner persistence adapter, owner profile, campaign topic analyzer, update pet service.

### 1.3 Web (`frontend/`)

| Area | Files |
|---|---|
| Shell / auth | `App.tsx` (pathname switch: `/partner*` → portal, else owner shell), `auth.tsx`, `partnerAuth.tsx`, `components/Layout.tsx` |
| Owner pages | `CityPage` (Paw City feed/map game UI, 507 lines), `DashboardPage`, `PetsPage`, `ActivitiesPage`, `BusinessesPage`, `InsightsPage`, `ProfilePage`, `AuthPage` |
| Partner pages | `PartnerPortal`, `PartnerLayout`, `PartnerAuthPage`, `PartnerProfilePage`, `PartnerCampaignsPage`, `PartnerBillingPage`, `PartnerFakeCheckoutPage` |
| Shared | `components/ui.tsx` (Button, Card, Badge, inputs, BrandLogo "PawMotion AI"), `components/petVisuals.tsx`, `components/PetForm.tsx`, `lib/validation.ts`, `api/client.ts` (hand-written fetch client), `types.ts` (hand-written DTO types) |
| Styles | `index.css`: brown/gold palette (`#825E31`, `#D99C52`…), Inter/Manrope, gradient `game-card` top border, `hud-chip`, `xp-track`, `city-grid` |
| Assets | `public/assets/dog-portrait.jpg`, `dog-walk.jpg` |
| Tests | none |

### 1.4 Mobile (`mobile/`)

`App.tsx` (text tab bar: Home, Pets, Activity, Insights, Profile), `src/screens/{Auth,Dashboard,Pets,Activities,Insights,Profile}Screen.tsx`, `src/components.tsx`, `src/petVisuals.tsx`, `src/theme.ts` (same brown palette), `src/api/client.ts`, `src/types.ts`, `src/validation.ts`, `src/auth.tsx` (AsyncStorage token). No navigation library, no tests, no push/location.

## 2. Gap analysis vs. the references and the brief

| Brief item | Today | Gap |
|---|---|---|
| Design system (cream/teal/coral, 24px radius, rounded bold type, AA) | Brown/gold palette, gradient accents, Inter/Manrope; duplicated in web CSS and `mobile/src/theme.ts` | New shared token package; delete old palette, gradients, `game-card`/`hud-chip`/`city-grid` styles |
| Bottom tab bar Feed · Map · + · AI · Profile | Web sidebar/top nav; mobile text tabs | Rewrite both shells |
| Feed: stories, typed reactions paw/laugh/heart, activity/achievement/sponsored cards, AI caption chip | Activity-only feed, reactions `LIKE/LOVE/CHEER`, promotions listed separately | Feed item model + ranking with sponsored interleaving; photo posts; stories |
| Create post bottom sheet (media, caption, pet tagging, AI caption) | none | Posts + media upload + AI caption endpoint |
| Map "Near me": parks, vets, cafes, lost pets, Playdate now | Partner + activity points (no categories, no radius) | Places, PostGIS radius search, lost-pet alerts, playdates |
| PetLife AI chat with actionable cards + Book | One-shot weekly insight | Conversations, suggestions, confirmed actions (bookings) |
| Pet profile: cover, level, stats, health passport, badges, weekly chart, 5 tabs, Follow/Playdate | Pet CRUD only | Follows, health passport, gamification read model, profile aggregate endpoint |
| Gamification: XP, levels, streaks, badges, weekly challenges, neighborhood leaderboard | Planned (T-012/T-013), not built | Full engagement module |
| Communities + events | none | New module |
| Landing page | none (login page is the entry) | New public route `/` |
| Partners: verified profile, campaign targeting/budget/schedule/formats/moderation/metrics, bookings | Campaign = title/description/audience/CTA, credits debit, AI topics, archive/expire | Extend campaign model, review workflow, ad formats, impressions/clicks/bookings metrics, partner services + bookings |
| Push notifications | none | Expo push tokens + dispatcher |
| OpenAPI-generated TS clients | hand-written types in two places | springdoc + `openapi-typescript` → `packages/api-client` |
| Tests | 7 unit tests | Testcontainers integration, Vitest/RTL, Playwright, Jest/RNTL |

## 3. Keep / rewrite / remove

### Keep (logic preserved, may be extended)
- Backend architecture (modular monolith, hexagonal, ADR-0001..0003), identity/JWT, security filters, error handler.
- **Ledger and billing** in full (wallets, accounts, locking, AbacatePay, webhooks, admin adjustments). Campaign creation keeps debiting credits.
- Pet, activity, insights domain and endpoints (activity becomes a feed item source; insights become one AI capability).
- Campaign topic analyzer (reused for relevance scoring).
- Flyway history V1–V6 (never edited; all changes are new migrations).
- Client validation rules (`lib/validation.ts`, `mobile/src/validation.ts`) — moved into a shared package.
- Docker Compose, CI structure.

### Rewrite
- Every web and mobile screen, both shells, partner portal UI, `ui.tsx`/`components.tsx`, `petVisuals`.
- `social` context → `feed` (Kotlin): typed reactions on any feed item, comments on any feed item, stories, ranking.
- `/api/map/points` → `/api/places/nearby` (+ lost pets, playdates) with PostGIS.
- `/api/promotions` and `/api/recommendations` → served through feed ranking (`SPONSORED` items), map (`promoted pins`), discover, and AI suggestions; old endpoints kept as deprecated aliases until clients migrate in phases 3–5, removed in phase 6.
- Hand-written `types.ts`/`api/client.ts` → generated types + thin typed fetch wrapper.
- Hand-written `docs/api/openapi.yaml` → exported from springdoc at build time.

### Remove
- Brown/gold palette, pink-orange gradients, gold/blue action colors, Inter/Manrope, `game-card`, `hud-chip`, `xp-track`, `city-grid`, `paw-city-bg`, `poppy-float`, `PoppyMark`, "PawMotion AI" branding in UI.
- "Paw City" naming in code/UI (`CityPage`, `share_to_paw_city` is renamed via migration to `shared_to_feed`).
- `DashboardPage`, `BusinessesPage`, `InsightsPage` as standalone pages (content folds into Feed/Profile/AI/Discover).
- Any "demo"/"investor" wording.

## 4. Target architecture

### 4.1 Packages

```
packages/
  design-tokens/   # TS source of truth: colors, type scale, radii, shadows, spacing; emits Tailwind @theme CSS + RN theme object
  api-client/      # openapi-typescript generated types + typed fetch helper shared by web and mobile
  shared/          # validation helpers, formatters (distance, duration, relative time), reaction metadata
frontend/          # landing (/), owner app (/app/*), partner dashboard (/partner/*), admin review (/admin/*)
mobile/            # Expo app: Feed, Map, Create (+), AI, Profile tabs
backend/
```

Packages are plain TypeScript consumed from source via `file:` dependencies (no build step); Metro gets `watchFolders`. See D-006.

### 4.2 New backend modules (Kotlin, same hexagonal layout)

| Module | Responsibility |
|---|---|
| `feed` | posts, media, stories, feed items, typed reactions, comments, ranking with sponsored interleaving |
| `health` | health passport: vaccines, deworming, vet visits, reminders, vet sharing links |
| `engagement` | XP ledger, levels, streaks, badges, weekly challenges, neighborhood leaderboard |
| `places` | pet-friendly places, check-ins, playdates, PostGIS radius search |
| `lostpets` | lost-pet alerts with location + radius, sightings, resolution |
| `assistant` | AI conversations, messages, suggestions, executed actions (confirm-before-execute), captions |
| `community` | communities (breed, neighborhood, cause), memberships, events, RSVPs |
| `booking` | partner services and bookings (vet, grooming, walker, pet hotel) |
| `ads` | campaign targeting, formats, moderation, placements, impressions/clicks, metrics |
| `notification` | Expo push tokens, notification preferences, dispatcher |
| `social` (follow graph) | pet follows, friends — folded into `feed` |

Existing Java modules stay Java (D-002).

## 5. Schema changes (Flyway V7+)

All migrations are additive first, then backfill, then constrain. Old columns are dropped only in phase 6 after clients no longer read them.

| Migration | Contents |
|---|---|
| `V7__postgis_and_profiles` | `CREATE EXTENSION IF NOT EXISTS postgis`; `owners.location geography(Point)` backfilled from lat/lng; `owners.plan` (`FREE`/`PREMIUM`), `owners.neighborhood`, `owners.role` adds `ADMIN`; `pets.avatar_url`, `pets.cover_url`, `pets.bio`, `pets.city` |
| `V8__feed_items_and_typed_reactions` | `media_assets`; `posts` (caption ≤ 200, author pet, visibility); `post_pet_tags`; `stories` (24h expiry); `feed_items` (`type` `POST`/`ACTIVITY`/`ACHIEVEMENT`/`SPONSORED`, `ref_id`, `author_owner_id`, `author_pet_id`, `published_at`); backfill one `ACTIVITY` feed item per `activities.share_to_paw_city = true`; `reactions` (`feed_item_id`, `owner_id`, `type` `PAW`/`LAUGH`/`HEART`, unique per owner+item) backfilled from `activity_reactions` (`LIKE→PAW`, `LOVE→HEART`, `CHEER→LAUGH`, original kept in `legacy_type`); `comments` backfilled from `activity_comments`; rename `share_to_paw_city` → `shared_to_feed`; `pet_follows` |
| `V9__health_passport` | `health_records` (`VACCINE`/`DEWORMING`/`VET_VISIT`/`MEDICATION`, name, administered_on, due_on, status, vet name/clinic), `health_reminders` (record, remind_at, channel, sent_at), `vet_share_links` (token hash, expires_at, revoked_at) |
| `V10__engagement` | `xp_events` (idempotent source key), `pet_levels` read model, `streaks`, `badges` (catalog seeded), `pet_badges`, `challenges` (weekly, target metric/value, author `ADMIN`/`AI`), `challenge_progress`; backfill XP/streaks/badges from existing activities |
| `V11__places_playdates` | `places` (category `PARK`/`VET`/`CAFE`/`HOTEL`/`GROOMING`/`PET_SHOP`, `geography(Point)`, rating, partner_id nullable, GIST index), `check_ins`, `playdates` (+ `playdate_invites`); partners with lat/lng backfilled into `places` |
| `V12__lost_pets` | `lost_pet_alerts` (pet, photo, last_seen `geography(Point)`, radius_m, status `ACTIVE`/`FOUND`/`CANCELLED`), `lost_pet_sightings` |
| `V13__assistant` | `ai_conversations`, `ai_messages` (role, content, suggestion payload JSONB), `ai_actions` (type `BOOKING`/`REMINDER`/`CAPTION`/`PLAYDATE`, status `PROPOSED`/`CONFIRMED`/`EXECUTED`/`FAILED`/`CANCELLED`, sponsored flag), `ai_usage` (monthly counters for FREE plan); backfill `insights` as assistant messages in a per-owner "Weekly insights" conversation |
| `V14__communities_events` | `communities` (kind `BREED`/`NEIGHBORHOOD`/`CAUSE`), `community_members`, `community_events` (location, starts_at), `event_rsvps` |
| `V15__partner_services_bookings` | `partner_profiles` (verified, verified_at, category, address, opening hours, cover) backfilled from `owners` partner fields; `partner_services`; `bookings` (owner, pet, service, slot, status `REQUESTED`/`CONFIRMED`/`CANCELLED`/`COMPLETED`, source `APP`/`AI`/`AD`, campaign_id) |
| `V16__campaigns_v2` | `partner_campaigns` adds `media_asset_id`, `body`, `cta_type` (`LINK`/`BOOKING`), `cta_url`, `service_id`, `budget_credits`, `starts_at`, `ends_at`, `target_location geography(Point)`, `target_radius_m`, `target_species[]`, `target_breeds[]`, `target_interests[]`, `formats[]` (`FEED_CARD`/`MAP_PIN`/`DISCOVER_HIGHLIGHT`/`NEARBY_PROMO`), `review_status`, `reviewed_by`, `reviewed_at`, `rejection_reason`, `end_reason`; status migrated `ARCHIVED→ENDED(end_reason=ARCHIVED)`, `EXPIRED→ENDED(end_reason=EXPIRED)`, `ACTIVE` stays `ACTIVE` (grandfathered as approved), `DRAFT` stays `DRAFT`; new statuses `IN_REVIEW`; `campaign_events` (impression/click/booking, owner hash, placement, occurred_at) + `campaign_daily_metrics`; `budget_credits = credits_charged`, `starts_at = created_at`, `ends_at = expires_at`, `title/description/cta` preserved |
| `V17__notifications` | `push_tokens` (owner, expo token, platform, last_seen), `notification_preferences`, `notifications` (type, payload, sent_at, error) |
| `V18__campaign_submitted_at` | `partner_campaigns.submitted_at` for review-queue ordering |
| `V19__drop_legacy` | dropped `activity_reactions`, `activity_comments`, `owners.business_name/website/partner_description/latitude/longitude` (all readers removed) |

## 6. API changes

All new routes: bean validation, `page`/`size` (max 50) with `{items,page,size,hasNext}` envelope, documented with springdoc annotations, authorization by role + ownership checks, per-user rate limit buckets on write/AI routes, structured JSON logs (`event`, `ownerId`, `route`, `latencyMs`).

| Screen | Endpoints |
|---|---|
| Feed | `GET /api/feed?page,size` (ranked, sponsored interleaved), `GET /api/stories`, `POST /api/stories`, `POST /api/feed/{itemId}/reactions {type}`, `DELETE /api/feed/{itemId}/reactions`, `GET/POST /api/feed/{itemId}/comments`, `POST /api/ads/events` (impression/click) |
| Create post | `POST /api/media` (multipart, image only, ≤ 8 MB), `POST /api/posts {petId, caption, mediaIds, taggedPetIds}`, `POST /api/assistant/captions {petId, mediaId?, hint?}` |
| Map | `GET /api/places/nearby?lat,lng,radius,categories[]` (includes promoted pins flagged `sponsored`), `GET /api/places/{id}`, `POST /api/places/{id}/check-ins`, `GET /api/lost-pets/nearby?lat,lng,radius`, `POST /api/lost-pets`, `POST /api/lost-pets/{id}/sightings`, `POST /api/lost-pets/{id}/resolve`, `POST /api/playdates` (now / scheduled), `POST /api/playdates/{id}/respond` |
| AI | `GET /api/assistant/conversations`, `POST /api/assistant/conversations/{id}/messages`, `POST /api/assistant/actions/{id}/confirm`, `POST /api/assistant/actions/{id}/cancel` |
| Pet profile | `GET /api/pets/{id}/profile` (cover, level, stats, badges, weekly chart, follow state), `GET /api/pets/{id}/posts`, `GET /api/pets/{id}/adventures`, `GET /api/pets/{id}/badges`, `GET /api/pets/{id}/friends`, `POST/DELETE /api/pets/{id}/follow`, health: `GET/POST /api/pets/{id}/health/records`, `PATCH /api/health/records/{id}`, `POST /api/pets/{id}/health/share` |
| Gamification | `GET /api/engagement/challenges`, `POST /api/engagement/challenges/{id}/join`, `GET /api/engagement/leaderboard?scope=neighborhood`, admin `POST /api/admin/challenges` |
| Communities | `GET /api/communities?kind,q`, `POST /api/communities/{id}/join`, `GET /api/communities/{id}/events`, `POST /api/communities/{id}/events`, `POST /api/events/{id}/rsvp` |
| Bookings | `GET /api/partners/{id}`, `GET /api/partners/{id}/services`, `POST /api/bookings`, `GET /api/bookings`, partner `GET /api/partner/bookings`, `POST /api/partner/bookings/{id}/{confirm,cancel}` |
| Partner ads | `GET/POST /api/partner/campaigns`, `PATCH /api/partner/campaigns/{id}`, `POST …/{id}/submit`, `POST …/{id}/pause`, `POST …/{id}/resume`, `POST …/{id}/end`, `GET …/{id}/metrics?from,to`, `GET/PATCH /api/partner/profile`, `GET/POST /api/partner/services` |
| Admin | `GET /api/admin/campaigns?status=IN_REVIEW`, `POST /api/admin/campaigns/{id}/{approve,reject}`, `POST /api/admin/partners/{id}/verify` |
| Push | `POST /api/devices/push-tokens`, `DELETE /api/devices/push-tokens/{token}`, `GET/PATCH /api/me/notification-preferences` |
| Discover | `GET /api/discover?lat,lng` (highlighted partners, communities, events) |

Deprecated endpoints removed in phase 6: `/api/activities/{id}/reactions|comments`, `/api/activities/feed`, `/api/map/points`, `/api/promotions`, `/api/recommendations`, and the v1 campaign `GET /analyze /archive` routes (v1 `POST /api/partner/campaigns` remains the create path).

### Feed ranking
1. Organic candidates: followed pets + own + neighborhood items from the last 14 days, scored by recency decay × (1 + log(1 + reactions + 2·comments)) with a boost for followed pets.
2. Sponsored candidates: `ACTIVE` approved campaigns with `FEED_CARD` format, inside schedule, matching viewer targeting (radius from viewer location, species/breed of viewer's pets, interests) with remaining budget; relevance = topic overlap (existing analyzer) × distance decay.
3. Interleave: at most one sponsored item per `ads.feed.max-frequency` organic items (default 5), never in position 0, never two in a row, no repeat of the same campaign within a page; impressions recorded when served.

## 7. Data migration plan
- Every migration is idempotent backfill SQL inside Flyway (no out-of-band scripts). Verified twice in CI via Testcontainers: (a) clean DB `V1→latest`, (b) a fixture DB migrated to `V6` and seeded with owners, partners, pets, activities, reactions, comments, campaigns in every status, wallets and payments, then migrated to latest with row-count and value assertions (no loss).
- Partners and campaigns: history (credits charged/refunded, ledger entries, payments) untouched; campaigns mapped to v2 fields as listed in `V16`.
- Reactions: 1:1 row migration with `legacy_type` retained.
- Activities shared to Paw City become `ACTIVITY` feed items with original timestamps.
- Insights become assistant messages (originals retained in `insights`).

## 8. Phase plan (one PR each)

| Phase | Deliverable | Exit check |
|---|---|---|
| 0 | This plan + `docs/decisions.md` | reviewed |
| 1 | `packages/design-tokens`, `packages/shared`, web Tailwind theme + component library, RN theme + components, design system guide (`docs/design/design-system.md`), old styles deleted | tokens consumed by both apps, typecheck/build green |
| 2 | Kotlin enabled, V7–V17, new modules, springdoc OpenAPI export, `packages/api-client` generation, rate limit + structured logs, Testcontainers suites | `gradle build` with integration tests green on clean + legacy fixture DB |
| 3 | Landing `/`, owner web app `/app/*` (Feed, Map, Create sheet, AI, Profile, Communities, Bookings) on generated client | Vitest/RTL + Playwright key flows green; 390×844 and 1440×900 screenshots vs reference |
| 4 | Expo app rewrite: tabs, bottom sheets, gestures, location/notification permissions, deep links, push registration | Jest/RNTL green, typecheck green |
| 5 | Partner dashboard (verified profile, campaigns v2 wizard, formats, services/bookings, metrics) + admin review UI | Playwright: create ad → review/approve → appears in feed + map → metrics update |
| 6 | Remove deprecated endpoints/columns/dead code, docs (README, OpenAPI, design system guide), final report | CI green; no old design anywhere |

## 9. Risks

| Risk | Mitigation |
|---|---|
| Scope is multi-team sized | Strict phase PRs; each phase is shippable; deprecated aliases keep old clients working between phases |
| Expo SDK 58 / RN 0.88 library compatibility (navigation, bottom sheet, maps) | Use Expo-maintained or widely used libs (`@react-navigation/*`, `react-native-gesture-handler`, `react-native-reanimated`, `expo-location`, `expo-notifications`, `react-native-maps`); pin versions via `npx expo install` |
| PostGIS required in tests | Testcontainers with `postgis/postgis:17-3.5` (same as Compose) |
| Data loss on enum/status changes | Additive migrations, legacy columns retained until phase 6, fixture-DB migration test |
| AI cost and safety | Server-side keys and prompts, plan quotas (FREE 10 msgs/month), deterministic fallback provider, actions require explicit confirmation, sponsored suggestions labeled |
| Ads harming feed quality | Frequency cap, relevance threshold, moderation before publish, per-campaign repeat cap |
| Location privacy (REQ-NFR-004) | Location only stored when user enables map/playdate/alert; nearby queries return rounded distances; lost-pet alert shows last-seen area, not owner address |
| Mixed Java/Kotlin build time and JPA quirks | `kotlin-spring` + `kotlin-jpa` plugins; new Kotlin modules use JdbcTemplate/JPA consistently with existing patterns |
| Push delivery cannot be fully tested in CI | Dispatcher behind a port; Expo HTTP adapter covered by WireMock-style stub test |
