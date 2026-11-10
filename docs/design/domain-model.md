# Domain Model

## Strategic model

PawMotion AI is divided into bounded contexts with explicit ownership. The modular monolith preserves these boundaries in code packages and database schemas/tables, while avoiding premature network-service decomposition.

```mermaid
flowchart LR
    Owner((Pet owner)) --> Identity[Identity Context]
    Owner --> Pet[Pet Context]
    Pet --> Activity[Activity Context]
    Activity --> AI[AI Insights Context]
    Activity --> Engagement[Engagement Context]
    Engagement --> Partner[Partner Context]
    Engagement --> Notification[Notification Context]
    Partner --> Ledger[Ledger Context]
    Billing[Billing Context] --> Ledger
    Activity --> Integration[Integration Context]
```

## Core aggregates

### Identity Context

- `Owner` aggregate: identity, credentials reference, display name, status.
- Value objects: `EmailAddress`, `OwnerId`.
- Invariants: email unique; owner can be disabled; credentials are never exposed by domain APIs.

### Pet Context

- `Pet` aggregate: identity, owner reference, species, birth date/age, size, preferences, lifecycle status.
- Value objects: `PetId`, `Species`, `PetSize`, `ActivityPreference`.
- Invariants: pet must belong to an owner; name required; age cannot be negative; inactive pets cannot receive new activities.

### Activity Context

- `Activity` aggregate: pet reference, activity type, source (`MANUAL`/`LIVE_TRACKING`), start time, duration, distance, optional notes, optional route (`ActivityRoute`: GeoJSON `Feature<LineString>`, start/end points), publish-to-Strava intent.
- `ActivityStatistics` read model: totals, averages, streaks, weekly progress.
- Value objects: `ActivityId`, `ActivityType`, `Distance`, `Duration`.
- Invariants: duration and distance non-negative; start time valid; pet must be active and owned by requester.

### AI Insights Context

- `Insight` aggregate: owner/pet scope, reporting period, generated text, provider metadata, validation status.
- Domain service: insight request validation and fallback selection.
- Invariants: no medical claims; response must match schema; fallback is deterministic.

### Engagement Context

- `WeeklyGoal`, `Streak`, `Badge`, `Challenge`, `ChallengeEnrollment`, `Progress`, `Reward`.
- Invariants: progress cannot exceed configured target unless explicitly allowed; completion is idempotent; badges are awarded once.

### Partner Context

- `BusinessProfile`, `PartnerPublicProfile` (categories, services, website, address, location), `Campaign`, `CampaignInteraction`, `CampaignDailyMetrics` (read model), `RewardOffer`, `Coupon`, `Redemption`.
- Value objects: `CampaignId`, `PartnerCategory`, `ServiceOffering`, `GeoPoint`, `InteractionType`.
- Invariants: coupon inventory cannot be oversold; redemption is idempotent; partner analytics are aggregate-only; a campaign stores `creditsCharged` and is created only if its ledger debit succeeds; publishing campaigns requires at least one category and one service.

### Billing Context

- `PaymentTransaction` aggregate: partner, amount, currency, CPP snapshot, expected credits, target ledger account, provider (AbacatePay), provider checkout id/url, idempotency key, status, version, timestamps.
- Value objects: `Money`, `CreditsPerCurrencyUnit`, `PaymentStatus`, `PaymentMethod`.
- Invariants: state machine `PENDING → CONFIRMED | FAILED | EXPIRED | CANCELED`, `CONFIRMED → DISPUTED | REFUNDED`, `DISPUTED → CONFIRMED | REFUNDED`; terminal states immutable; credits granted at most once and only on `CONFIRMED`.

### Ledger Context

- `Wallet` aggregate (one per partner, created at partner registration, status `ACTIVE`/`BLOCKED`) containing `LedgerAccount`s `MAIN`, `BONUS`, `PARTNER`; `LedgerEntry` append-only records. Wallet and accounts carry a JPA `@Version`; postings also lock account rows.
- Value objects: `Credits` (positive integer), `EntryDirection` (CREDIT/DEBIT), `LedgerReference` (type + id).
- Invariants: balance never negative; balance equals sum of entries; `(accountId, referenceType, referenceId, direction)` unique (split debits/refunds post one entry per account); closed accounts reject postings; debits consume `MAIN` → `BONUS` → `PARTNER` (split if needed); blocked wallets reject debits for new campaigns and purchases.

### Integration Context

- `StravaConnection` aggregate (owner, athlete id, encrypted tokens, scopes, auto-publish flag, status) and `StravaPublication` (activity id, status, Strava activity id, attempts, last error).
- Invariants: one active connection per owner; one publication per activity per connection; tokens never exposed.

## Major user journeys

```mermaid
sequenceDiagram
    actor Owner
    participant API
    participant Identity
    participant Pet
    participant Activity
    participant Dashboard
    Owner->>API: Register
    API->>Identity: Create owner
    Owner->>API: Login
    API-->>Owner: JWT
    Owner->>API: Create pet
    API->>Pet: Persist pet
    Owner->>API: Record activity
    API->>Activity: Validate and persist
    Owner->>API: Open dashboard
    API->>Dashboard: Aggregate owned data
    Dashboard-->>Owner: Statistics and progress
```

## Activity recording flow

```mermaid
flowchart TD
    A[Submit activity] --> B{Authenticated?}
    B -- No --> X[401]
    B -- Yes --> C{Pet owned and active?}
    C -- No --> Y[403/404]
    C -- Yes --> D{Values valid?}
    D -- No --> Z[422]
    D -- Yes --> E[Persist activity]
    E --> F[Publish ActivityRecorded event]
    F --> G[Update projections asynchronously]
```

## Challenge completion flow

```mermaid
flowchart TD
    A[Activity recorded] --> B[Challenge progress evaluator]
    B --> C{Threshold met?}
    C -- No --> D[Continue]
    C -- Yes --> E{Already completed?}
    E -- Yes --> F[No-op/idempotent]
    E -- No --> G[Mark complete]
    G --> H[Issue reward]
    H --> I[Publish ChallengeCompleted event]
```

## Coupon redemption flow

```mermaid
flowchart TD
    A[Redeem coupon] --> B{Owner owns coupon?}
    B -- No --> X[403]
    B -- Yes --> C{Active and not redeemed?}
    C -- No --> Y[409/410]
    C -- Yes --> D{Inventory available?}
    D -- No --> Z[409]
    D -- Yes --> E[Atomic redeem]
    E --> F[Publish CouponRedeemed event]
```

## AI insight generation flow

```mermaid
flowchart TD
    A[Insight request] --> B[Load authorized aggregates]
    B --> C[Build validated prompt input]
    C --> D{AI enabled and provider available?}
    D -- No --> E[Deterministic fallback]
    D -- Yes --> F[Call provider with timeout/retry]
    F --> G{Valid non-medical response?}
    G -- No --> E
    G -- Yes --> H[Persist insight metadata]
    E --> I[Return insight]
    H --> I
```

## Credit purchase flow (REQ-BILL-001/002)

```mermaid
sequenceDiagram
    actor Partner
    participant API
    participant Billing
    participant PSP as Payment provider
    participant Ledger
    Partner->>API: POST /partner/billing/purchases (amount, method, Idempotency-Key)
    API->>Billing: create PENDING tx (cppSnapshot, expectedCredits)
    Billing->>PSP: createCheckout
    PSP-->>Partner: PIX QR / hosted card form
    PSP->>API: signed webhook (paid)
    API->>Billing: verify signature, dedupe event
    Note over Billing,Ledger: single DB transaction
    Billing->>Billing: PENDING -> CONFIRMED
    Billing->>Ledger: credit(account, expectedCredits, ref=PAYMENT:txId)
    Ledger-->>Billing: entry + new balance
    Billing->>Billing: outbox CreditsPurchased.v1
```

## Campaign creation with credit debit (REQ-CAMP-003)

```mermaid
flowchart TD
    A[POST /partner/campaigns + Idempotency-Key] --> B{Same key already processed?}
    B -- Yes --> R[Return existing campaign]
    B -- No --> C[Begin DB transaction]
    C --> D[Lock ledger account FOR UPDATE]
    D --> E{balance >= CAMPAIGN_CREATION_COST_CREDITS?}
    E -- No --> X[Rollback, 402 InsufficientCredits]
    E -- Yes --> F[Insert campaign with creditsCharged]
    F --> G[Insert DEBIT entry ref=CAMPAIGN:id, update balance]
    G --> H[Outbox CampaignCreditsDebited.v1]
    H --> I[Commit, 201 with remainingBalance]
```

## Campaign click to partner detail (REQ-CAMP-001, REQ-DISC-001)

```mermaid
sequenceDiagram
    actor Owner
    participant App as Owner app
    participant API
    participant Partner
    Owner->>App: Tap campaign card
    App->>API: POST /campaigns/{id}/clicks [fire-and-forget]
    API->>Partner: store interaction, upsert daily metrics
    App->>API: GET /partners/{partnerId}
    API-->>App: public profile, services, website?, location?, active campaigns
    App-->>Owner: Partner detail (services emphasized if no website, map if located)
```

## Live tracking and Strava publication (REQ-TRACK-001, REQ-STRAVA-002)

```mermaid
stateDiagram-v2
    [*] --> Idle
    Idle --> Tracking: Start walk (Home quick action)
    Tracking --> Paused: Pause
    Paused --> Tracking: Resume
    Tracking --> Review: Finish
    Paused --> Review: Finish
    Review --> Saved: Save (POST /activities with route)
    Review --> Discarded: Discard
    Saved --> PublishQueued: publishToStrava = true
    PublishQueued --> Published: Strava upload ok
    PublishQueued --> PublishFailed: permanent error
    Saved --> [*]
    Discarded --> [*]
```
