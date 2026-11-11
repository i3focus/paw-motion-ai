# Task Dependencies

```mermaid
flowchart LR
    T001[T-001 Docs baseline] --> T002[T-002 Backend scaffold]
    T001 --> T003[T-003 Frontend scaffold]
    T002 --> T004[T-004 Docker/CI]
    T003 --> T004
    T002 --> T005[T-005 Auth]
    T005 --> T006[T-006 Pets]
    T006 --> T007[T-007 Activities]
    T007 --> T008[T-008 Dashboard]
    T008 --> T009[T-009 Frontend screens]
    T008 --> T010[T-010 AI fallback]
    T010 --> T011[T-011 AI provider]
    T008 --> T012[T-012 Goals]
    T012 --> T013[T-013 Challenges/rewards]
    T013 --> T014[T-014 Partner portal]
    T005 --> T015[T-015 Hardening]
    T002 --> T016[T-016 Pricing config]
    T016 --> T017[T-017 Ledger]
    T017 --> T018[T-018 Billing + fake PSP]
    T018 --> T019[T-019 Webhook + real PSP]
    T017 --> T020[T-020 Campaign debit]
    T018 --> T021[T-021 Wallet UI]
    T020 --> T021
    T014 --> T022[T-022 Click tracking]
    T022 --> T023[T-023 Partner dashboard]
    T020 --> T023
    T014 --> T024[T-024 Public profile + location]
    T022 --> T025[T-025 Partner detail]
    T024 --> T025
    T024 --> T026[T-026 Partner map]
    T007 --> T027[T-027 Activity routes]
    T027 --> T028[T-028 Live tracking]
    T002 --> T029[T-029 Outbox]
    T005 --> T030[T-030 Strava OAuth]
    T028 --> T031[T-031 Strava publish]
    T020 --> T034[T-034 Expiry + archive refund]
    T016 --> T035[T-035 Credits explainer]
    T011 --> T036[T-036 Admin/AI challenges]
    T019 --> T037[T-037 Wallet unblock]
    T005 --> T038[T-038 Deletion + retention]
    T029 --> T031
    T030 --> T031
```

## Critical path

Auth → Pet → Activity → Dashboard → Frontend integration. AI, gamification, and partner work should not block this path.

CR-2026-11 critical path: Pricing config → Ledger → Campaign debit → Partner dashboard. Payment provider selection (open question 11) gates T-019 only; the fake provider unblocks everything else.

## External dependencies

- PostgreSQL + PostGIS container image.
- Optional RabbitMQ container image.
- Optional AI provider credentials.
- Optional OIDC provider for production-like auth.
- Payment provider sandbox account with PIX/card and webhook signing secret (T-019).
- Strava API application (client id/secret, redirect URI) (T-030).
- OSM-based tile provider respecting usage policy; optional geocoding service (T-024, T-026).
- Expo development build for background location and native maps (T-026, T-028).

---

> **Status note (2026-11):** refactor phases all merged; dependency order recorded
> in `docs/refactor-report.md`. File retained for the original spec-phase record.
