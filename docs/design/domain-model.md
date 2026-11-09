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

- `Activity` aggregate: pet reference, activity type, start time, duration, distance, optional notes/route summary.
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

- `BusinessProfile`, `Campaign`, `RewardOffer`, `Coupon`, `Redemption`.
- Invariants: coupon inventory cannot be oversold; redemption is idempotent; partner analytics are aggregate-only.

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
