# PawMotion AI — Requirements Analysis

## Scope

PawMotion AI is an AI-powered pet activity and wellness companion for the HackBrasil 2026 MVP. This analysis treats the README plus the attached spec-driven development prompt as the available PRD. The actual product PRD was not included as a separate attachment, so requirement IDs below were derived from the visible MVP feature list. Decision (2026-11-10): there is no separate PRD; **this document is the authoritative source of requirements** and is updated through change requests.

## Actors

- **Pet owner**: registers, manages pets, records activities, views progress, receives insights and rewards.
- **Business partner**: manages campaigns and views authorized aggregate analytics.
- **Admin**: platform operator; creates challenges/badges, unblocks partners, manages adjustments.
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
- **Dependencies**: Identity context. Decision: the hackathon uses the platform's own JWT issuer (ADR-0008); external OIDC/Keycloak is deferred.
- **Test scenarios**: valid registration, duplicate email, invalid password, login success/failure.

### REQ-PET-001 — Pet profile management

- **Priority**: Must
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner
- **Main flow**: Owner creates a pet profile with name, species, age/birth date, size, and activity preferences; owner can list and retrieve owned pets.
- **Business rules**: Pet belongs to exactly one owner; species-specific constraints are validated; pet cannot be accessed by another owner. Enums (as implemented): species `DOG`, `CAT`, `BIRD`, `RABBIT`, `OTHER`; size `SMALL`, `MEDIUM`, `LARGE`.
- **Acceptance criteria**: CRUD operations are owner-scoped and persisted.
- **Dependencies**: Identity context.
- **Test scenarios**: create valid pet, reject invalid species/size, list only owned pets, object-level authorization.

### REQ-ACTIVITY-001 — Activity recording and history

- **Priority**: Must
- **Actor**: Pet owner
- **Preconditions**: Authenticated owner with at least one pet
- **Main flow**: Owner records walk/run/play activity with pet, type, start time, duration, distance, optional notes; system validates and persists it.
- **Business rules**: Activity types (as implemented): `WALK`, `RUN`, `HIKE`, `BIKE_RIDE`, `SWIM`, `PLAY`. GPS traces are captured only by the mobile app (REQ-TRACK-001) and stored as GeoJSON; web records manual activities. Distance/duration cannot be negative; start time cannot be in the future beyond a small clock-skew allowance; activity must reference an owned pet.
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
- **Business rules**: No diagnosis or medical advice; provider failure never blocks dashboard usage. Decision: OpenAI is the primary demo provider; the deterministic fallback remains for local/CI.
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
- **Main flow**: Owner joins an active challenge, progress is tracked from activities, completion awards a reward (badge and/or coupon).
- **Business rules**: Completion is evaluated server-side and idempotent. A challenge is one-to-many: any number of owner profiles can join and complete it; the same badge can be awarded to many owners (once per owner).
- **Acceptance criteria**: Join/progress/complete flow works end-to-end.
- **Dependencies**: Engagement context.
- **Test scenarios**: join, progress, duplicate completion, expired challenge.

### REQ-CHAL-002 — Challenge and badge authoring by Admin or AI

- **Priority**: Should
- **Actor**: Admin, System (AI)
- **Preconditions**: Authenticated `ADMIN`, or scheduled AI generation job.
- **Main flow**: Admin creates a challenge with title, description, metric (`DISTANCE_METERS`, `DURATION_SECONDS`, `ACTIVITY_COUNT`), target, period (e.g. current week), eligible activity types, and reward badge — e.g. "Walk 3 km this week and earn the Walker badge". Alternatively the AI provider proposes challenges from aggregate (non-personal) activity statistics; AI proposals are saved as `DRAFT` and published by an Admin (or auto-published when `CHALLENGE_AI_AUTOPUBLISH=true`).
- **Business rules**: Badges are reusable definitions (name, icon, description) awarded to many owners; challenge metrics are validated (positive target, end after start); AI output is schema-validated and must not contain medical claims.
- **Acceptance criteria**: An Admin-created "Walk 3 km this week" challenge can be joined by multiple owners, each owner reaching 3 km of `WALK` in the period earns the Walker badge exactly once.
- **Proposed API**: `POST/GET /api/admin/challenges`, `POST /api/admin/challenges/{id}/publish`, `POST/GET /api/admin/badges`, `GET /api/challenges`, `POST /api/challenges/{id}/enrollments`.
- **Dependencies**: Engagement context, AI Insights provider port, `ADMIN` role.
- **Test scenarios**: admin create/publish, AI draft validation, many owners completing, badge idempotency, non-admin forbidden.

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
- **Acceptance criteria**: Dashboard shows campaign metrics without PII. Decision: the safe aggregate exposed to partners is the counter of owner views (clicks) per campaign — total and unique owners — without any owner identity.
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

| `CAMPAIGN_DURATION_DAYS` | integer > 0 | Expiration time of every campaign, independent of credits spent | `30` |
| `ABACATEPAY_API_KEY` | secret | AbacatePay API key (`abc_dev_…` test mode, `abc_prod_…` production) | — |
| `ABACATEPAY_WEBHOOK_SECRET` | secret | Secret configured on the AbacatePay webhook | — |
| `ABACATEPAY_CREDIT_PRODUCT_EXTERNAL_ID` | string | `externalId` of the AbacatePay product priced at 1 currency unit | `pawmotion-credits-brl` |
| `PARTNER_PORTAL_BASE_URL` | URL | Used for checkout `completionUrl`/`returnUrl` | `http://localhost:5173/partner` |

**CPP rule**: one unit of fiat currency is worth CPP credits, so `credits = floor(amountPaid × CPP)` as an integer (computed with `BigDecimal`, stored as `BIGINT`). `CAMPAIGN_PRICE_AMOUNT` is the fiat reference shown to partners and `CAMPAIGN_CREATION_COST_CREDITS` is what is debited; both are configured independently and the application logs a startup warning when `CAMPAIGN_CREATION_COST_CREDITS != floor(CAMPAIGN_PRICE_AMOUNT × CPP)`. Startup validation fails fast when any value is missing, non-positive, or malformed. There are no predefined credit packs.

### REQ-BILL-001 — Credit purchase

- **Priority**: Must
- **Actor**: Business partner, Payment provider
- **Preconditions**: Authenticated partner with a wallet.
- **Main flow**: Partner opens "Buy credits", enters a fiat amount in whole BRL, sees the credits to be granted (`floor(amount × CPP)`), and confirms. System creates a `PaymentTransaction` in `PENDING` (idempotency key, CPP snapshot, expected credits, target account `MAIN`), then creates an **AbacatePay hosted checkout** (`POST /v2/checkouts/create`) with `items = [{id: ABACATEPAY_CREDIT_PRODUCT_EXTERNAL_ID, quantity: amount}]` (product priced at 100 cents), `methods = [PIX, CARD]`, `externalId = transactionId`, `metadata = {partnerId, transactionId}`, `completionUrl`/`returnUrl` pointing to the partner portal, and redirects the partner to the returned `url` where they pay by PIX or card (ADR-0015).
- **Alternative flows**: amount below `BILLING_MIN_PURCHASE_AMOUNT` (never below AbacatePay's R$ 1,00 minimum) → `422`; repeated submit with same idempotency key → returns the existing transaction (AbacatePay also deduplicates by `externalId`); provider unavailable → transaction `FAILED`, no credits, partner can retry; partner returns without paying → transaction stays `PENDING` until `BILLING_PENDING_TTL` (default 24 h); the expiry job re-fetches the checkout, confirms it if paid, otherwise deletes it (`POST /v2/checkouts/delete`) and marks the transaction `EXPIRED`; blocked wallet → `423 Locked`.
- **Business rules**: Credits are granted only after the payment is confirmed (REQ-BILL-002). The CPP used is the value at transaction creation, stored on the transaction, so later configuration changes never alter in-flight purchases. Card data never touches PawMotion servers (hosted checkout/tokenization only). Amounts use decimal arithmetic; credits are integers rounded down.
- **Acceptance criteria**: Given CPP = 10, when a partner starts a purchase of BRL 50.00, then a `PENDING` transaction exists with `expectedCredits = 500` and no balance change.
- **Proposed API**: `GET /api/partner/billing/pricing`, `POST /api/partner/billing/purchases` (header `Idempotency-Key`), `GET /api/partner/billing/purchases`, `GET /api/partner/billing/purchases/{id}`.
- **Dependencies**: Billing context, PaymentGatewayPort, REQ-CONF-001, REQ-LEDGER-001.
- **Test scenarios**: credit preview rounding, minimum amount, idempotent create, provider error, CPP snapshot after config change.

### REQ-BILL-002 — Payment confirmation and transaction control

- **Priority**: Must
- **Actor**: Payment provider, System
- **Preconditions**: `PENDING` payment transaction.
- **Main flow**: AbacatePay sends `checkout.completed` to `POST /api/webhooks/payments/abacatepay`. System validates `X-Webhook-Secret` (constant-time compare) and the `X-Webhook-Signature` HMAC-SHA256 of the raw body, rejects events whose `devMode` does not match the environment, deduplicates by envelope `id`, re-fetches the checkout (`GET /v2/checkouts/get?externalId=`) to confirm `status = PAID` and `paidAmount`, transitions the transaction `PENDING → CONFIRMED`, and in the **same database transaction** posts a ledger credit of `expectedCredits` to the target wallet account (REQ-LEDGER-002) and records an outbox event `CreditsPurchased.v1`.
- **Alternative flows**: `FAILED`/`EXPIRED`/`CANCELED` close the transaction without credits; duplicate or out-of-order webhooks are no-ops (respond `2xx`); invalid secret/signature → `401` and audit log; `checkout.disputed` → `DISPUTED` and the partner wallet is `BLOCKED` until resolution (dispute won → back to `CONFIRMED`, unblocked if no other reason); `checkout.refunded` or `checkout.lost` → `REFUNDED`, a compensating debit of the purchased credits up to the available balance, and the **partner is blocked** (wallet `BLOCKED`, campaign creation and purchases rejected, active campaigns paused) until an Admin settles and unblocks.
- **Business rules**: Allowed transitions: `PENDING → CONFIRMED | FAILED | EXPIRED | CANCELED`, `CONFIRMED → DISPUTED | REFUNDED`, `DISPUTED → CONFIRMED | REFUNDED`, `EXPIRED → CONFIRMED` (a verified late payment is always credited). Webhook processing is fast and idempotent (AbacatePay retries non-2xx up to 7 times over ~18 h and requires a response within 30 s); manual re-sends carry a new event id, so idempotency is also enforced by transaction id. Terminal states are immutable. One payment transaction credits the ledger at most once (unique constraint on ledger reference `PAYMENT:{transactionId}`). A reconciliation job compares `PENDING` transactions older than a threshold with the provider.
- **Acceptance criteria**: Given a `PENDING` purchase of 500 credits, when the same confirmation webhook arrives twice concurrently, then the balance increases by exactly 500 and one ledger entry exists.
- **Proposed API**: `POST /api/webhooks/payments/{provider}` (public, signature-verified).
- **Dependencies**: REQ-BILL-001, REQ-LEDGER-002.
- **Test scenarios**: confirm, fail, expire, duplicate webhook, concurrent webhooks, bad signature, refund with and without sufficient balance, reconciliation.

### REQ-LEDGER-001 — Partner wallet and accounts

- **Priority**: Must
- **Actor**: Business partner, System
- **Preconditions**: Partner account exists.
- **Main flow**: When a partner registers, the system creates (in the registration transaction) a wallet with three accounts: `MAIN` (purchased credits), `BONUS` (promotional credits granted by the platform), and `PARTNER` (credits granted through partnership agreements). Existing partners get the same on migration.
- **Business rules**: Exactly one wallet per partner; account codes unique within a wallet; wallet status `ACTIVE` or `BLOCKED`; accounts are never deleted, only closed. Credit purchases always credit `MAIN`; `BONUS`/`PARTNER` are credited only by Admin adjustments. **Debit order**: `MAIN` → `BONUS` → `PARTNER`; a debit larger than one account's balance is split across accounts in that order within the same transaction, and fails if the total available balance is insufficient.
- **Acceptance criteria**: A new partner sees a wallet with `MAIN`, `BONUS`, and `PARTNER` accounts at balance `0`; given MAIN 300, BONUS 300, PARTNER 0 and a 500-credit debit, MAIN ends at 0 and BONUS at 100; a partner can only see their own wallet.
- **Proposed API**: `GET /api/partner/wallet` (accounts and balances).
- **Dependencies**: Identity (`PartnerId`), Ledger context.
- **Test scenarios**: wallet auto-creation idempotency, multiple accounts, closed account rejection, cross-partner access denied.

### REQ-LEDGER-002 — Atomic credit and debit postings

- **Priority**: Must
- **Actor**: System
- **Preconditions**: Open ledger account.
- **Main flow**: Every balance change is a ledger posting `{accountId, direction (CREDIT|DEBIT), amount, reference type, reference id, idempotency key, occurredAt}`. Posting and balance update happen in one ACID database transaction using **both** pessimistic locking (`SELECT … FOR UPDATE` on the accounts being posted) and optimistic locking (`@Version` on every ledger/billing JPA entity, checked on update) as defined in ADR-0014.
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

### REQ-LEDGER-004 — Credits explainer for partners

- **Priority**: Must
- **Actor**: Business partner
- **Main flow**: The partner portal explains the credits model before the first purchase and in a permanent "How credits work" section: what a credit is, the current CPP (`1 BRL = CPP credits`), the cost of a campaign in credits and its fiat reference, campaign duration, the three accounts and the debit order, archive refunds, and that credits do not expire and are non-withdrawable.
- **Business rules**: All values come from `GET /api/partner/billing/pricing`; no hard-coded numbers in the UI.
- **Acceptance criteria**: A partner without purchases sees the explainer and a credit preview calculator before buying.

### REQ-CAMP-003 — Campaign creation consumes credits

- **Priority**: Must
- **Actor**: Business partner
- **Preconditions**: Authenticated partner with a wallet.
- **Main flow**: Partner fills the campaign form; the UI shows the cost (`CAMPAIGN_CREATION_COST_CREDITS`, plus `CAMPAIGN_PRICE_AMOUNT` as fiat reference) and current balance. On submit, the system in one database transaction creates the campaign with `expiresAt = now + CAMPAIGN_DURATION_DAYS` and posts ledger debits (split `MAIN` → `BONUS` → `PARTNER`) with reference `CAMPAIGN:{campaignId}`, then records outbox event `CampaignCreditsDebited.v1`.
- **Alternative flows**: insufficient credits → `402 Payment Required` Problem Details with `requiredCredits` and `balance`, no campaign persisted, UI offers "Buy credits"; retried request with the same `Idempotency-Key` → returns the already-created campaign without a second debit.
- **Business rules**: Every campaign creation triggers exactly one debit operation. Cost is read from configuration at creation time and stored on the campaign (`creditsCharged`, plus the per-account split). Editing, analyzing, pausing, or resuming a campaign does not debit credits. A `BLOCKED` wallet rejects creation with `423`. Archiving refunds credits (REQ-CAMP-004).
- **Acceptance criteria**: Given balance 600 and cost 500, when a campaign is created, then the campaign exists, balance is 100, and the statement shows a debit referencing the campaign; given balance 400, the request fails with `402` and nothing changes.
- **Proposed API**: existing `POST /api/partner/campaigns` gains `Idempotency-Key` header and optional `debitAccountId`; response includes `creditsCharged` and `remainingBalance`.
- **Dependencies**: REQ-LEDGER-002, REQ-CONF-001, Partner context.
- **Test scenarios**: success, insufficient credits, idempotent retry, concurrent creations, config change between quote and submit.

### REQ-CAMP-004 — Campaign expiration and archive refund

- **Priority**: Must
- **Actor**: Business partner, System
- **Main flow**: Every campaign has `expiresAt`. `expiresAt` is fixed at creation (when credits are debited) regardless of status. A scheduled job moves `DRAFT`/`ACTIVE`/`PAUSED` campaigns past `expiresAt` to `EXPIRED` (hidden from owners, read-only), and activation/resume is rejected when `now ≥ expiresAt`. When a partner archives a non-expired campaign, the system refunds credits in one transaction with reference `CAMPAIGN_REFUND:{campaignId}` back to the accounts that were debited (reverse order `PARTNER` → `BONUS` → `MAIN`, capped by each account's original debit).
- **Business rules**: Refund amount = `floor(creditsCharged × remainingTime / totalDuration)` (prorated by unused time; see open question 16 to confirm vs. full refund); expired campaigns are not refunded; a campaign is refunded at most once (unique ledger reference); archive is final.
- **Acceptance criteria**: Archiving a 30-day, 500-credit campaign after 15 days returns 250 credits and shows a refund entry in the statement; an expired campaign cannot be archived for refund.
- **Proposed API**: `POST /api/partner/campaigns/{campaignId}/archive` (response includes `creditsRefunded`).
- **Test scenarios**: expiration job, prorated refund, double archive, refund to split accounts, blocked wallet.

### REQ-CONF-001 — Environment-configured pricing

- **Priority**: Must
- **Actor**: Operator
- **Main flow**: CPP, campaign fiat price, campaign credit cost, currency, minimum purchase, and payment provider are read from environment variables listed above and exposed read-only through `GET /api/partner/billing/pricing`.
- **Business rules**: No pricing value is hard-coded or editable through the UI. Changes take effect on restart and only for new transactions/campaigns (snapshots protect existing ones).
- **Acceptance criteria**: Changing `CAMPAIGN_CREATION_COST_CREDITS` and restarting changes the cost of subsequent campaigns only; invalid values prevent startup.
- **Test scenarios**: binding/validation, snapshot behavior.

### REQ-CAMP-001 — Campaign click tracking

- **Priority**: Must
- **Actor**: Pet owner (implicit), System
- **Preconditions**: Campaign `ACTIVE`.
- **Main flow**: When the owner taps a campaign card (web or mobile) the app records a click (a "view" of the campaign) and navigates to the partner detail (REQ-DISC-001). Clicks are sent to `POST /api/campaigns/{campaignId}/clicks` with source (`WEB`, `MOBILE`) and placement (`DASHBOARD`, `INSIGHTS`, `MAP`, `PARTNER_PAGE`). Impressions are out of scope for now.
- **Business rules**: Interactions are stored without exposing owner identity to partners. Duplicate clicks from the same owner on the same campaign within a dedup window (default 30 min, configurable) count once as unique click but are kept as raw total. Tracking failures never block navigation. Interactions on non-active campaigns are ignored.
- **Acceptance criteria**: Tapping a campaign card increments the campaign click count once per dedup window and opens the partner detail.
- **Dependencies**: Partner context (or Campaign Analytics read model), owner apps.
- **Test scenarios**: click, dedup window, inactive campaign, unauthenticated request rejected, rate limit.

### REQ-CAMP-002 — Partner campaign performance dashboard

- **Priority**: Must
- **Actor**: Business partner
- **Preconditions**: Authenticated partner.
- **Main flow**: Partner opens "Dashboard" in the partner area and sees, per campaign and in total, for a selectable period (7/30/90 days or custom): total clicks (views), unique owners who clicked (distinct over the whole period, not summed daily uniques), clicks per day (time series chart), clicks by placement/source, credits spent, cost per click (credits spent / clicks), active/expired campaigns with `expiresAt`, and current wallet balances with a "Buy credits" shortcut.
- **Business rules**: Aggregate-only counters; no owner identifiers, demographics, or PII. Partner sees only their campaigns. Metrics are near-real-time (≤ 5 min lag acceptable).
- **Acceptance criteria**: Metrics match recorded interactions for the period; empty state explains how to create the first campaign and buy credits.
- **Proposed API**: `GET /api/partner/analytics/summary?from=&to=`, `GET /api/partner/analytics/campaigns/{campaignId}?from=&to=&granularity=day`.
- **Dependencies**: REQ-CAMP-001, REQ-LEDGER-003.
- **Test scenarios**: aggregation correctness, date boundaries/time zone, authorization, empty state.

### REQ-PARTNER-002 — Partner public business profile

- **Priority**: Must
- **Actor**: Business partner
- **Preconditions**: Authenticated partner.
- **Main flow**: Partner maintains a public profile: business name, logo, description, area of operation (categories: `VET_CLINIC` (pet vet), `PET_SHOP`, `FEED_STORE` (casa de ração), `PET_LABORATORY`, `GROOMING`, `DOG_WALKER`, `TRAINING`, `DAYCARE_HOTEL`, `OTHER`), services offered (list of name + optional short description), website (optional), contact phone/WhatsApp (optional), address, and geolocation, plus optional service radius. The address is geocoded automatically (`GeocodingPort`); when geocoding succeeds the partner appears on the owner map without extra steps, otherwise the partner can set a manual pin.
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
- **Main flow**: The mobile Home (dashboard) shows a prominent "Start walk" quick-action button (Strava-style). If Strava is not connected, the first use prompts the owner to connect Strava (REQ-STRAVA-001), with "Not now" allowed. Tapping it asks for location permission if needed, lets the owner confirm the pet (defaults to last used) and activity type (walk/run/play), then starts live tracking showing elapsed time, distance, pace, and the route on a map. Owner can pause, resume, and finish. On finish, the owner reviews a summary (optional notes, "Publish to Strava" toggle) and saves; the system creates an activity with start time, duration (excluding pauses), distance, and route.
- **Alternative flows**: permission denied → offer manual activity entry; GPS signal lost → keep timer, mark gap; app backgrounded or screen locked → tracking continues in background (background location permission required; Expo development build); every GPS point and the finished activity are persisted locally on the device (GeoJSON) until successfully sent to the backend and to Strava; no network on save → kept in the local queue and synced automatically later; discard → nothing persisted; activity shorter than the minimum (e.g. 60 s) → warn before saving.
- **Business rules**: GPS points with poor accuracy (> configurable threshold) or impossible speed are filtered; distance computed from filtered points; only one tracking session at a time; existing activity rules (REQ-ACTIVITY-001) apply.
- **Acceptance criteria**: From Home, an owner starts tracking in at most two taps, and saving produces an activity with route and correct duration/distance visible in history and dashboard.
- **Proposed API**: existing `POST /api/activities` extended with optional `route` (GeoJSON LineString with timestamps) and `source = MANUAL | LIVE_TRACKING`.
- **Dependencies**: REQ-ACTIVITY-001, REQ-TRACK-002, Expo location/maps.
- **Test scenarios**: start/pause/resume/finish, permission denied, offline save and sync, point filtering, background behavior.

### REQ-TRACK-002 — GPS route persistence and visualization

- **Priority**: Must
- **Actor**: Pet owner
- **Main flow**: Activities with routes store the GeoJSON `Feature<LineString>` (with per-point timestamps) sent by the mobile app in a JSONB column, plus start/end points; activity detail on web and mobile renders the route on a MapLibre map directly from the GeoJSON. PostGIS conversion is deferred until spatial queries are needed.
- **Business rules**: Routes are owner-private; simplification preserves total distance within 2%; maximum points per route bounded.
- **Acceptance criteria**: A tracked activity displays its route on both clients; another owner cannot access it.
- **Dependencies**: PostGIS, REQ-TRACK-001.
- **Test scenarios**: persistence, simplification, authorization.

### REQ-STRAVA-001 — Strava account connection

- **Priority**: Should
- **Actor**: Pet owner, Strava
- **Preconditions**: Authenticated owner.
- **Main flow**: The app asks the owner to connect Strava (first live tracking and Profile). Owner taps "Connect Strava", completes Strava OAuth with scope `activity:write`, and the system stores the athlete id and encrypted access/refresh tokens. Owner can set "Auto-publish tracked activities" (proposed default: on after connecting) and disconnect at any time.
- **Alternative flows**: owner denies consent or scope → not connected; token expired → refreshed transparently; refresh fails or deauthorization received → connection marked revoked and owner informed.
- **Business rules**: Tokens encrypted at rest, never returned to clients or logged; OAuth `state` parameter validated; disconnect revokes tokens at Strava and deletes them locally.
- **Proposed API**: `GET /api/integrations/strava/authorize-url`, `GET /api/integrations/strava/callback`, `GET/DELETE /api/integrations/strava`, `PATCH /api/integrations/strava/preferences`.
- **Dependencies**: Integration context, StravaPort, Strava API application credentials (`STRAVA_CLIENT_ID`, `STRAVA_CLIENT_SECRET`, `STRAVA_REDIRECT_URI`, `INTEGRATION_TOKEN_ENCRYPTION_KEY`).
- **Test scenarios**: connect, invalid state, refresh, revoke, disconnect.

### REQ-STRAVA-002 — Publish activity to Strava

- **Priority**: Should
- **Actor**: Pet owner, System
- **Preconditions**: Strava connected; activity saved.
- **Main flow**: When an activity is saved with "Publish to Strava" (or auto-publish is on), the system enqueues a publication through the outbox; a worker uploads it to Strava (GPX upload when a route exists, manual activity otherwise) with name/description mentioning the pet (e.g. "Walk with Thor · PawMotion AI") and proposed `sport_type` mapping `WALK→Walk`, `RUN→Run`, `HIKE→Hike`, `BIKE_RIDE→Ride`, `SWIM→Swim`, `PLAY→Workout` and stores the Strava activity id and status (`PENDING`, `PUBLISHED`, `FAILED`). Owner can also publish an existing activity from its detail.
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
- **Retention (recommended, pending legal review)**: account deletion is a soft delete with a 30-day grace period (owner can cancel), followed by hard delete/anonymization of personal data (profile, pets, activities, routes, insights, Strava tokens) within 15 days; application access logs (IP, timestamp) kept 6 months as required for application providers by the Marco Civil da Internet (Lei 12.965/2014, art. 15); payment transactions and ledger entries kept 5 years with personal fields anonymized (tax/civil limitation periods); backups rotate within 35 days so deleted data disappears from them.
- **Acceptance criteria**: Security tests cover unauthenticated access, cross-owner access, and validation failures.

### REQ-NFR-002 — Operability

- **Priority**: Must
- **Business rules**: Health checks, structured logs, metrics, trace correlation, Docker Compose local setup, migrations, CI.
- **Acceptance criteria**: A developer can clone, configure `.env`, run Docker Compose, and exercise the MVP. Decision: Docker (Compose) is the primary deployment target; managed platforms are optional.

## MVP scope conflicts and risks

1. The attached document is a development master prompt, not the detailed product PRD. Requirement IDs are provisional.
2. The target stack mentions Spring Boot 4.x and Spring AI; version compatibility must be verified before implementation.
3. The demo requires identity, pet, activity, AI, gamification, partner portal, and coupon redemption. A two-person hackathon team should deliver the Register → Pet → Activity → Dashboard slice first.
4. PostGIS is desired for route mapping, but the first activity slice can store optional GeoJSON coordinates without requiring advanced spatial queries.
5. Paid AI APIs must not be required for local development or CI; deterministic test doubles are mandatory.
6. CR-2026-11 introduces real money flows through AbacatePay. Its Modo Teste (`abc_dev_` keys, simulated payments) is used for the demo; production requires AbacatePay account verification (CNPJ). A deterministic `fake` provider is mandatory for CI. Webhooks need a public HTTPS URL (use a tunnel when running on local Docker).
7. Cross-context atomicity between Partner (campaign) and Ledger (debit) is resolved with a single local database transaction inside the modular monolith (ADR-0014); splitting these contexts into services later would require a reservation/saga pattern.
8. Background GPS tracking (required) and native MapLibre maps require an Expo development build (not Expo Go) and OS background-location permission review; this affects the mobile demo workflow.
9. Strava publication requires a registered Strava API application and is subject to Strava rate limits and API terms; it must stay optional and asynchronous.
