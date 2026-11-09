# PawMotion AI — Requirements Analysis

## Scope

PawMotion AI is an AI-powered pet activity and wellness companion for the HackBrasil 2026 MVP. This analysis treats the README plus the attached spec-driven development prompt as the available PRD. The actual product PRD was not included as a separate attachment, so requirement IDs below are derived from the visible MVP feature list and are provisional until the product PRD is supplied.

## Actors

- **Pet owner**: registers, manages pets, records activities, views progress, receives insights and rewards.
- **Business partner**: manages campaigns and views authorized aggregate analytics.
- **System**: enforces ownership, generates insights, evaluates goals/challenges, and records events.

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
