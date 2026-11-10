# PawMotion AI — Requirements Analysis

## Scope

PawMotion AI is an AI-powered pet activity and wellness companion for the HackBrasil 2026 MVP. This analysis treats the README plus the attached spec-driven development prompt as the available PRD. The actual product PRD was not included as a separate attachment, so requirement IDs below are derived from the visible MVP feature list and are provisional until the product PRD is supplied.

## Actors

- **Pet owner**: registers, manages pets, records activities, views progress, receives insights and rewards.
- **Business partner**: manages campaigns and views authorized aggregate analytics.
- **System**: enforces ownership, generates insights, evaluates goals/challenges, and records events.
- **Payment provider** (external): processes fiat payments (PIX, card) and notifies payment outcomes.
- **Strava** (external): receives activities published by owners who connected their Strava account.

## Requirements

### REQ-AUTH-001 — Owner registration and authentication

- **Priority**: Must
- **Actor**: Pet owner
- **Preconditions**: None
- **Main flow**: Owner submits email, password, and display name; system validates uniqueness, hashes the password, persists the owner, and returns an authentication token.
- **Alternative flows**: Duplicate email returns `409`; weak password returns `422`.
- **Business rules**: Email is unique case-insensitively; passwords are never logged or returned.
- **Acceptance criteria**: A registered owner can authenticate and access only their own resources.
- **Dependencies**: Identity context, JWT/OIDC decision.
- **Test scenarios**: valid registration, duplicate email, invalid password, login success/failure.

### REQ-PET-001 — Pet profile management

- **Priority**: Must
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner
- **Main flow**: Owner creates a pet profile with name, species, age/birth date, size, and activity preferences; owner can list and retrieve owned pets.
- **Business rules**: Pet belongs to exactly one owner; species-specific constraints are validated; pet cannot be accessed by another owner.
- **Acceptance criteria**: CRUD operations are owner-scoped and persisted.
- **Dependencies**: Identity context.
- **Test scenarios**: create valid pet, reject invalid species/size, list only owned pets, object-level authorization.

### REQ-ACTIVITY-001 — Activity recording and history

- **Priority**: Must
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner with at least one pet
- **Main flow**: Owner records walk/run/play activity with pet, type, start time, duration, distance, optional notes; system validates and persists it.
- **Business rules**: Distance/duration cannot be negative; start time cannot be in the future beyond a small clock-skew allowance; activity must reference an owned pet.
- **Acceptance criteria**: Activities can be created, listed, filtered, and used for dashboard statistics.
- **Dependencies**: Pet context.
- **Test scenarios**: valid activity, invalid pet ownership, negative values, future timestamp, filtering by pet/date.

### REQ-DASH-001 — Dashboard statistics

- **Priority**: Must
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner
- **Main flow**: Dashboard returns pet count, recent activities, weekly totals, distance, duration, and streak indicators for the current owner.
- **Business rules**: Statistics are calculated from persisted activities only; no cross-owner leakage.
- **Acceptance criteria**: Values match recorded activities and empty states are valid.
- **Dependencies**: Activity context.
- **Test scenarios**: empty owner, multiple pets, date boundary, streak calculation.

### REQ-AI-001 — AI activity insight

- **Priority**: Must for demo, replaceable provider
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner with activity statistics
- **Main flow**: System builds a validated activity aggregate, invokes an AI provider through a port, validates the response, rejects medical claims, persists metadata, and returns a deterministic fallback on failure.
- **Business rules**: No diagnosis or medical advice; provider failure never blocks dashboard usage.
- **Acceptance criteria**: Endpoint returns either provider insight or clearly marked fallback with provider metadata.
- **Dependencies**: Activity context, AI Insights context.
- **Test scenarios**: provider success, timeout, invalid response, medical-claim rejection, fallback.

### REQ-GAME-001 — Goals, streaks, and badges

- **Priority**: Should
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner
- **Main flow**: Owner sets a weekly activity goal; system calculates streaks and awards badges when thresholds are met.
- **Business rules**: Goal progress is idempotent and derived from activity records.
- **Acceptance criteria**: Dashboard displays goal progress, streak, and earned badges.
- **Dependencies**: Engagement context.
- **Test scenarios**: goal completion, streak continuity, badge idempotency.

### REQ-CHAL-001 — Challenges

- **Priority**: Should
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner
- **Main flow**: Owner joins an active challenge, progress is tracked from activities, completion awards a reward.
- **Business rules**: Completion is evaluated server-side and idempotent.
- **Acceptance criteria**: Join/progress/complete flow works end-to-end.
- **Dependencies**: Engagement context.
- **Test scenarios**: join, progress, duplicate completion, expired challenge.

### REQ-REWARD-001 — Coupons and redemption

- **Priority**: Should
- **Actor**: Pet owner, business partner
- **Preconditions**: Reward issued by challenge completion or campaign
- **Main flow**: Owner redeems a coupon; system validates ownership, expiry, campaign limits, and inventory atomically.
- **Business rules**: No overselling; redemption is idempotent; one coupon cannot be redeemed twice by same owner.
- **Acceptance criteria**: Concurrent redemption attempts cannot exceed inventory.
- **Dependencies**: Engagement and Partner contexts.
- **Test scenarios**: successful redemption, expired coupon, duplicate redemption, concurrent limit enforcement.

### REQ-PARTNER-001 — Business campaign dashboard

- **Priority**: Should
- **Actor**: Business partner
- **Preconditions**: Authenticated partner linked to campaign
- **Main flow**: Partner creates campaign, partners with reward offers, and views aggregate campaign analytics.
- **Business rules**: Partners receive aggregate analytics only, never individual owner activity histories.
- **Acceptance criteria**: Dashboard shows campaign metrics without PII.
- **Dependencies**: Partner context.
- **Test scenarios**: authorization, aggregate-only response, campaign metrics.
- **Refinement (CR-2026-11)**: campaign creation is now paid with credits (REQ-CAMP-003) and the analytics part is detailed by REQ-CAMP-001/REQ-CAMP-002.

## Change request CR-2026-11 — Partner monetization, partner discovery, and live tracking

Source: product feedback received on 2026-11-10. It supersedes the earlier "partner pays a campaign package" idea: partners **buy credits**, and **each campaign creation debits credits** from the partner wallet through a dedicated Ledger domain.

### Glossary

- **Credit**: indivisible internal unit (integer) used to pay for platform features. Credits are not money and are not withdrawable.
- **CPP (credits per currency unit)**: conversion rate from fiat to credits. Paying `1 BRL` yields `CPP` credits. Configured by environment variable.
- **Wallet**: container of one or more ledger accounts owned by a partner.
- **Ledger account**: balance-holding account inside a wallet (e.g. `MAIN`, `BONUS`). Its balance is the sum of its immutable ledger entries.
- **Ledger entry / posting**: immutable, append-only record of a credit or debit on an account, linked to a business reference (payment, campaign).
- **Payment transaction**: record of a fiat payment attempt with a payment provider and its lifecycle status.
- **Impression / click**: an owner seeing / tapping a campaign card in the web or mobile owner app.

### Configuration (environment variables)

| Variable | Type | Meaning | Example |
|---|---|---|---|
| `BILLING_CURRENCY` | ISO-4217 | Fiat currency accepted for credit purchases | `BRL` |
| `BILLING_CREDITS_PER_CURRENCY_UNIT` | decimal > 0 | CPP: credits granted per 1 unit of fiat currency | `10` |
| `BILLING_MIN_PURCHASE_AMOUNT` | decimal > 0 | Minimum fiat amount per purchase | `10.00` |
| `CAMPAIGN_PRICE_AMOUNT` | decimal > 0 | Fiat reference price of one campaign, shown to partners | `50.00` |
| `CAMPAIGN_CREATION_COST_CREDITS` | integer > 0 | Credits debited when a campaign is created | `500` |
| `PAYMENT_PROVIDER` | enum | Active payment adapter (`fake`, provider id) | `fake` |

Startup validation fails fast when any value is missing, non-positive, or malformed. If `CAMPAIGN_CREATION_COST_CREDITS != CAMPAIGN_PRICE_AMOUNT × CPP` the application logs a configuration warning (see open question 13).

### REQ-BILL-001 — Credit purchase

- **Priority**: Must
- **Actor**: Business partner, Payment provider
- **Preconditions**: Authenticated partner with a wallet.
- **Main flow**: Partner opens "Buy credits", enters a fiat amount (or picks a suggested pack), sees the credits to be granted (`floor(amount × CPP)`), chooses a payment method (PIX or card), and confirms. System creates a `PaymentTransaction` in `PENDING` with an idempotency key, the CPP snapshot, expected credits, and target ledger account, then redirects/returns the provider checkout (PIX QR code or hosted card form).
- **Alternative flows**: amount below `BILLING_MIN_PURCHASE_AMOUNT` → `422`; repeated submit with same idempotency key → returns the existing transaction; provider unavailable → transaction `FAILED`, no credits, partner can retry.
- **Business rules**: Credits are granted only after the payment is confirmed (REQ-BILL-002). The CPP used is the value at transaction creation, stored on the transaction, so later configuration changes never alter in-flight purchases. Card data never touches PawMotion servers (hosted checkout/tokenization only). Amounts use decimal arithmetic; credits are integers rounded down.
- **Acceptance criteria**: Given CPP = 10, when a partner starts a purchase of BRL 50.00, then a `PENDING` transaction exists with `expectedCredits = 500` and no balance change.
- **Proposed API**: `GET /api/partner/billing/pricing`, `POST /api/partner/billing/purchases` (header `Idempotency-Key`), `GET /api/partner/billing/purchases`, `GET /api/partner/billing/purchases/{id}`.
- **Dependencies**: Billing context, PaymentGatewayPort, REQ-CONF-001, REQ-LEDGER-001.
- **Test scenarios**: credit preview rounding, minimum amount, idempotent create, provider error, CPP snapshot after config change.

### REQ-BILL-002 — Payment confirmation and transaction control

- **Priority**: Must
- **Actor**: Payment provider, System
- **Preconditions**: `PENDING` payment transaction.
- **Main flow**: Provider sends a signed webhook (or the system polls). System verifies signature, deduplicates by provider event id, transitions the transaction `PENDING → CONFIRMED`, and in the **same database transaction** posts a ledger credit of `expectedCredits` to the target wallet account (REQ-LEDGER-002) and records an outbox event `CreditsPurchased.v1`.
- **Alternative flows**: `FAILED`/`EXPIRED`/`CANCELED` outcomes close the transaction without credits; duplicate or out-of-order webhooks are no-ops; invalid signature → `401` and audit log; a later chargeback/refund → `REFUNDED` and a compensating ledger debit (balance may not go negative; if insufficient, account is flagged and further campaign creation blocked — see open question 15).
- **Business rules**: Allowed transitions: `PENDING → CONFIRMED | FAILED | EXPIRED | CANCELED`, `CONFIRMED → REFUNDED`. Terminal states are immutable. One payment transaction credits the ledger at most once (unique constraint on ledger reference `PAYMENT:{transactionId}`). A reconciliation job compares `PENDING` transactions older than a threshold with the provider.
- **Acceptance criteria**: Given a `PENDING` purchase of 500 credits, when the same confirmation webhook arrives twice concurrently, then the balance increases by exactly 500 and one ledger entry exists.
- **Proposed API**: `POST /api/webhooks/payments/{provider}` (public, signature-verified).
- **Dependencies**: REQ-BILL-001, REQ-LEDGER-002.
- **Test scenarios**: confirm, fail, expire, duplicate webhook, concurrent webhooks, bad signature, refund with and without sufficient balance, reconciliation.

### REQ-LEDGER-001 — Partner wallet and accounts

- **Priority**: Must
- **Actor**: Business partner, System
- **Preconditions**: Partner account exists.
- **Main flow**: When a partner registers (or on first billing access for existing partners) the system creates a wallet with a default `MAIN` account. A wallet may hold one or more accounts (e.g. `MAIN`, `BONUS`/promotional); each account has its own balance.
- **Business rules**: Exactly one wallet per partner; account codes unique within a wallet; exactly one default account; accounts are never deleted, only closed (closed accounts reject new postings). Credit purchases target the default account unless another open account is chosen. Campaign debits use the account selected by the partner, else the default-debit policy (see open question 14).
- **Acceptance criteria**: A new partner sees a wallet with one `MAIN` account and balance `0`; a partner can only see their own wallet.
- **Proposed API**: `GET /api/partner/wallet` (accounts and balances).
- **Dependencies**: Identity (`PartnerId`), Ledger context.
- **Test scenarios**: wallet auto-creation idempotency, multiple accounts, closed account rejection, cross-partner access denied.

### REQ-LEDGER-002 — Atomic credit and debit postings

- **Priority**: Must
- **Actor**: System
- **Preconditions**: Open ledger account.
- **Main flow**: Every balance change is a ledger posting `{accountId, direction (CREDIT|DEBIT), amount, reference type, reference id, idempotency key, occurredAt}`. Posting and balance update happen in one ACID database transaction with row-level locking (or optimistic version check with retry) on the account.
- **Business rules**: Entries are append-only and immutable; corrections are compensating entries. Amounts are positive integers. A debit that would make the balance negative is rejected (`InsufficientCreditsException`) and nothing is written. `(referenceType, referenceId, direction)` is unique, making postings idempotent. The cached account balance must always equal the sum of its entries (verified by a consistency check). Each entry stores `balanceAfter` for statements.
- **Acceptance criteria**: Given balance 500 and two concurrent campaign creations costing 500 each, exactly one succeeds and the final balance is 0.
- **Dependencies**: Ledger context, PostgreSQL transactions.
- **Test scenarios**: credit, debit, insufficient balance, duplicate reference, concurrent debits (Testcontainers), balance/entry consistency.

### REQ-LEDGER-003 — Wallet statement

- **Priority**: Should
- **Actor**: Business partner
- **Preconditions**: Authenticated partner.
- **Main flow**: Partner views balances per account and a paginated statement of entries (date, description, reference — purchase or campaign — credit/debit, balance after) and the list of payment transactions with their status.
- **Business rules**: Partner-scoped; read-only; amounts shown in credits with fiat reference where applicable.
- **Acceptance criteria**: Statement entries reconcile to the displayed balance.
- **Proposed API**: `GET /api/partner/wallet/accounts/{accountId}/entries?page=&size=`.
- **Dependencies**: REQ-LEDGER-001/002.
- **Test scenarios**: pagination, ordering, isolation between partners.

### REQ-CAMP-003 — Campaign creation consumes credits

- **Priority**: Must
- **Actor**: Business partner
- **Preconditions**: Authenticated partner with a wallet.
- **Main flow**: Partner fills the campaign form; the UI shows the cost (`CAMPAIGN_CREATION_COST_CREDITS`, plus `CAMPAIGN_PRICE_AMOUNT` as fiat reference) and current balance. On submit, the system in one database transaction creates the campaign and posts a ledger debit with reference `CAMPAIGN:{campaignId}`, then records outbox event `CampaignCreditsDebited.v1`.
- **Alternative flows**: insufficient credits → `402 Payment Required` Problem Details with `requiredCredits` and `balance`, no campaign persisted, UI offers "Buy credits"; retried request with the same `Idempotency-Key` → returns the already-created campaign without a second debit.
- **Business rules**: Every campaign creation triggers exactly one debit. Cost is read from configuration at creation time and stored on the campaign (`creditsCharged`). Editing, analyzing, pausing, or resuming a campaign does not debit credits. Archiving does not refund (see open question 16).
- **Acceptance criteria**: Given balance 600 and cost 500, when a campaign is created, then the campaign exists, balance is 100, and the statement shows a debit referencing the campaign; given balance 400, the request fails with `402` and nothing changes.
- **Proposed API**: existing `POST /api/partner/campaigns` gains `Idempotency-Key` header and optional `debitAccountId`; response includes `creditsCharged` and `remainingBalance`.
- **Dependencies**: REQ-LEDGER-002, REQ-CONF-001, Partner context.
- **Test scenarios**: success, insufficient credits, idempotent retry, concurrent creations, config change between quote and submit.

### REQ-CONF-001 — Environment-configured pricing

- **Priority**: Must
- **Actor**: Operator
- **Main flow**: CPP, campaign fiat price, campaign credit cost, currency, minimum purchase, and payment provider are read from environment variables listed above and exposed read-only through `GET /api/partner/billing/pricing`.
- **Business rules**: No pricing value is hard-coded or editable through the UI. Changes take effect on restart and only for new transactions/campaigns (snapshots protect existing ones).
- **Acceptance criteria**: Changing `CAMPAIGN_CREATION_COST_CREDITS` and restarting changes the cost of subsequent campaigns only; invalid values prevent startup.
- **Test scenarios**: binding/validation, snapshot behavior.

### REQ-CAMP-001 — Campaign impression and click tracking

- **Priority**: Must
- **Actor**: Pet owner (implicit), System
- **Preconditions**: Campaign `ACTIVE`.
- **Main flow**: When owner apps (web and mobile) render a campaign card they record an impression; when the owner taps it they record a click and navigate to the partner detail (REQ-DISC-001). Events are sent to `POST /api/campaigns/{campaignId}/interactions` with `type = IMPRESSION | CLICK`, source (`WEB`, `MOBILE`), and placement (`DASHBOARD`, `INSIGHTS`, `MAP`, `PARTNER_PAGE`).
- **Business rules**: Interactions are stored without exposing owner identity to partners. Duplicate clicks from the same owner on the same campaign within a dedup window (default 30 min, configurable) count once as unique click but are kept as raw total. Tracking failures never block navigation. Interactions on non-active campaigns are ignored.
- **Acceptance criteria**: Tapping a campaign card increments the campaign click count once per dedup window and opens the partner detail.
- **Dependencies**: Partner context (or Campaign Analytics read model), owner apps.
- **Test scenarios**: impression, click, dedup window, inactive campaign, unauthenticated request rejected, rate limit.

### REQ-CAMP-002 — Partner campaign performance dashboard

- **Priority**: Must
- **Actor**: Business partner
- **Preconditions**: Authenticated partner.
- **Main flow**: Partner opens "Dashboard" in the partner area and sees, per campaign and in total, for a selectable period (7/30/90 days or custom): impressions, clicks, unique clicks, CTR, clicks per day (time series chart), clicks by placement/source, credits spent, cost per click (credits spent / clicks), active campaigns, and current wallet balance with a "Buy credits" shortcut.
- **Business rules**: Aggregate-only data; no owner identifiers or PII. Breakdowns with fewer than a minimum number of unique owners (k-anonymity threshold, default 5) are grouped as "other". Partner sees only their campaigns. Metrics are near-real-time (≤ 5 min lag acceptable).
- **Acceptance criteria**: Metrics match recorded interactions for the period; empty state explains how to create the first campaign and buy credits.
- **Proposed API**: `GET /api/partner/analytics/summary?from=&to=`, `GET /api/partner/analytics/campaigns/{campaignId}?from=&to=&granularity=day`.
- **Dependencies**: REQ-CAMP-001, REQ-LEDGER-003.
- **Test scenarios**: aggregation correctness, date boundaries/time zone, k-anonymity grouping, authorization, empty state.

### REQ-PARTNER-002 — Partner public business profile

- **Priority**: Must
- **Actor**: Business partner
- **Preconditions**: Authenticated partner.
- **Main flow**: Partner maintains a public profile: business name, logo, description, area of operation (categories such as vet clinic, pet shop, grooming, dog walking, training, daycare), services offered (list of name + optional short description), website (optional), contact phone/WhatsApp (optional), address, and geolocation (lat/long set by geocoding the address or by pinning on a map), plus optional service radius.
- **Business rules**: At least one category and one service are required to publish campaigns; website must be a valid `https` URL when present; coordinates must be valid WGS84; only the owning partner can edit.
- **Acceptance criteria**: Profile changes are reflected on the owner-facing partner detail and map.
- **Proposed API**: extend `GET/PATCH /api/me` for partners or add `GET/PUT /api/partner/profile/public`.
- **Dependencies**: Identity/Partner contexts, PostGIS, geocoding port (optional).
- **Test scenarios**: validation of URL/coordinates/categories, authorization, geocoding failure fallback to manual pin.

### REQ-DISC-001 — Partner detail from a campaign ad

- **Priority**: Must
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner; campaign `ACTIVE`.
- **Main flow**: Owner taps a campaign card (dashboard, insights, or map). The app records a click (REQ-CAMP-001) and opens the partner detail: business name, logo, area of operation, description, campaign highlighted, website button, contact, address with an embedded location map, "Open in maps" action, and the partner's other active campaigns.
- **Alternative flows**: no website → the page emphasizes the "Services offered" section (no broken/empty website link); no geolocation → map section hidden and address shown as text.
- **Business rules**: Only public profile fields are exposed; partner private data (email, billing, wallet) never returned.
- **Acceptance criteria**: For a partner without website, the detail shows the services list prominently and no website action.
- **Proposed API**: `GET /api/partners/{partnerId}` (public profile + active campaigns).
- **Dependencies**: REQ-PARTNER-002, REQ-CAMP-001.
- **Test scenarios**: with/without website, with/without location, inactive partner, field exposure.

### REQ-DISC-002 — Partner map and campaign list

- **Priority**: Should
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner.
- **Main flow**: Owner opens "Partners" (web and mobile) with two views: a map (MapLibre + OpenStreetMap) with markers for partners that have active campaigns, centered on the owner's location (with permission) or a default city, and a list of active campaigns. Owner can filter by category and distance; tapping a marker shows a preview card that opens the partner detail.
- **Business rules**: Nearby search uses PostGIS distance queries with a bounded radius and pagination; owner location is used only for the query and not persisted.
- **Acceptance criteria**: Partners with active campaigns inside the visible area appear on the map and in the list; selecting one opens REQ-DISC-001.
- **Proposed API**: `GET /api/partners/nearby?lat=&lng=&radiusKm=&category=`, `GET /api/campaigns/active?category=`.
- **Dependencies**: REQ-PARTNER-002, PostGIS.
- **Test scenarios**: radius filter, category filter, location denied fallback, no results.

### REQ-TRACK-001 — Quick-start live activity tracking (mobile)

- **Priority**: Must
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner on the mobile app with at least one active pet.
- **Main flow**: The mobile Home (dashboard) shows a prominent "Start walk" quick-action button (Strava-style). Tapping it asks for location permission if needed, lets the owner confirm the pet (defaults to last used) and activity type (walk/run/play), then starts live tracking showing elapsed time, distance, pace, and the route on a map. Owner can pause, resume, and finish. On finish, the owner reviews a summary (optional notes, "Publish to Strava" toggle) and saves; the system creates an activity with start time, duration (excluding pauses), distance, and route.
- **Alternative flows**: permission denied → offer manual activity entry; GPS signal lost → keep timer, mark gap; app backgrounded → continue tracking when background permission is granted, otherwise warn; no network on save → store locally and sync later; discard → nothing persisted; activity shorter than the minimum (e.g. 60 s) → warn before saving.
- **Business rules**: GPS points with poor accuracy (> configurable threshold) or impossible speed are filtered; distance computed from filtered points; only one tracking session at a time; existing activity rules (REQ-ACTIVITY-001) apply.
- **Acceptance criteria**: From Home, an owner starts tracking in at most two taps, and saving produces an activity with route and correct duration/distance visible in history and dashboard.
- **Proposed API**: existing `POST /api/activities` extended with optional `route` (GeoJSON LineString with timestamps) and `source = MANUAL | LIVE_TRACKING`.
- **Dependencies**: REQ-ACTIVITY-001, REQ-TRACK-002, Expo location/maps.
- **Test scenarios**: start/pause/resume/finish, permission denied, offline save and sync, point filtering, background behavior.

### REQ-TRACK-002 — GPS route persistence and visualization

- **Priority**: Must
- **Actor**: Pet owner
- **Main flow**: Activities with routes store a simplified LineString (PostGIS) plus start/end points; activity detail on web and mobile renders the route on a map.
- **Business rules**: Routes are owner-private; simplification preserves total distance within 2%; maximum points per route bounded.
- **Acceptance criteria**: A tracked activity displays its route on both clients; another owner cannot access it.
- **Dependencies**: PostGIS, REQ-TRACK-001.
- **Test scenarios**: persistence, simplification, authorization.

### REQ-STRAVA-001 — Strava account connection

- **Priority**: Should
- **Actor**: Pet owner, Strava
- **Preconditions**: Authenticated owner.
- **Main flow**: In Profile, owner taps "Connect Strava", completes Strava OAuth with scope `activity:write`, and the system stores the athlete id and encrypted access/refresh tokens. Owner can set "Auto-publish tracked activities" and disconnect at any time.
- **Alternative flows**: owner denies consent or scope → not connected; token expired → refreshed transparently; refresh fails or deauthorization received → connection marked revoked and owner informed.
- **Business rules**: Tokens encrypted at rest, never returned to clients or logged; OAuth `state` parameter validated; disconnect revokes tokens at Strava and deletes them locally.
- **Proposed API**: `GET /api/integrations/strava/authorize-url`, `GET /api/integrations/strava/callback`, `GET/DELETE /api/integrations/strava`, `PATCH /api/integrations/strava/preferences`.
- **Dependencies**: Integration context, StravaPort, Strava API application credentials (`STRAVA_CLIENT_ID`, `STRAVA_CLIENT_SECRET`, `STRAVA_REDIRECT_URI`, `INTEGRATION_TOKEN_ENCRYPTION_KEY`).
- **Test scenarios**: connect, invalid state, refresh, revoke, disconnect.

### REQ-STRAVA-002 — Publish activity to Strava

- **Priority**: Should
- **Actor**: Pet owner, System
- **Preconditions**: Strava connected; activity saved.
- **Main flow**: When an activity is saved with "Publish to Strava" (or auto-publish is on), the system enqueues a publication through the outbox; a worker uploads it to Strava (GPX upload when a route exists, manual activity otherwise) with name/description mentioning the pet (e.g. "Walk with Thor · PawMotion AI") and stores the Strava activity id and status (`PENDING`, `PUBLISHED`, `FAILED`). Owner can also publish an existing activity from its detail.
- **Alternative flows**: Strava rate limit or 5xx → retry with exponential backoff; 4xx validation → `FAILED` with reason shown; duplicate request → no second upload.
- **Business rules**: Publication is asynchronous and never blocks saving the activity; each activity is published at most once per connection; respects Strava API rate limits and terms.
- **Acceptance criteria**: A tracked walk with the toggle on appears in the owner's Strava feed and the app shows "Published to Strava" with a link.
- **Proposed API**: `POST /api/activities/{activityId}/strava-publication`, publication status included in activity responses.
- **Dependencies**: REQ-STRAVA-001, REQ-TRACK-001, outbox (ADR-0006).
- **Test scenarios**: publish with/without route, retry, permanent failure, idempotency, disconnected account.

### REQ-NFR-003 — Financial integrity and auditability

- **Priority**: Must
- **Business rules**: Ledger is append-only and double-checked by a balance consistency job; all payment state changes and ledger postings are audited with correlation ids; money uses `BigDecimal`/`NUMERIC`, credits use `BIGINT`; idempotency keys on every money-moving endpoint; webhook signatures verified; PCI scope minimized via hosted checkout; financial records retained per fiscal rules even after account deletion (anonymized).
- **Acceptance criteria**: Concurrency and idempotency tests prove no double credit/debit and no negative balance.

### REQ-NFR-004 — Location privacy

- **Priority**: Must
- **Business rules**: Owner GPS routes and current location are personal data (LGPD): explicit permission, owner-only access, never shared with partners, excluded from partner analytics, deleted on account deletion; Strava publication only with explicit opt-in.
- **Acceptance criteria**: Security tests prove partners and other owners cannot access routes or owner locations.

### REQ-NFR-001 — Security and privacy

- **Priority**: Must
- **Actor**: All
- **Business rules**: JWT validation, RBAC, object-level ownership checks, input validation, secure secrets, no credentials in Git, LGPD-oriented minimization, account deletion workflow.
- **Acceptance criteria**: Security tests cover unauthenticated access, cross-owner access, and validation failures.

### REQ-NFR-002 — Operability

- **Priority**: Must
- **Business rules**: Health checks, structured logs, metrics, trace correlation, Docker Compose local setup, migrations, CI.
- **Acceptance criteria**: A developer can clone, configure `.env`, run Docker Compose, and exercise the MVP.

## MVP scope conflicts and risks

1. The attached document is a development master prompt, not the detailed product PRD. Requirement IDs are provisional.
2. The target stack mentions Spring Boot 4.x and Spring AI; version compatibility must be verified before implementation.
3. The demo requires identity, pet, activity, AI, gamification, partner portal, and coupon redemption. A two-person hackathon team should deliver the Register → Pet → Activity → Dashboard slice first.
4. PostGIS is desired for route mapping, but the first activity slice can store optional GeoJSON coordinates without requiring advanced spatial queries.
5. Paid AI APIs must not be required for local development or CI; deterministic test doubles are mandatory.
6. CR-2026-11 introduces real money flows. A payment provider (PIX/card) must be chosen and a sandbox account provisioned before REQ-BILL-* can be demoed; a deterministic `fake` provider is mandatory for local development and CI.
7. Cross-context atomicity between Partner (campaign) and Ledger (debit) is resolved with a single local database transaction inside the modular monolith (ADR-0014); splitting these contexts into services later would require a reservation/saga pattern.
8. Live GPS tracking in background and native MapLibre maps require an Expo development build (not Expo Go) on mobile; this affects the mobile demo workflow.
9. Strava publication requires a registered Strava API application and is subject to Strava rate limits and API terms; it must stay optional and asynchronous.
