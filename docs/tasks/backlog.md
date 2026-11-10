# Backlog

| ID | Task | Requirements | Context | Complexity | Dependencies | Status |
|---|---|---|---:|---|---|---|
| T-001 | Create repo structure and docs baseline | NFR-002 | Platform | S | — | Done |
| T-002 | Scaffold Spring Boot modular monolith | NFR-002 | Platform | M | T-001 | Done |
| T-003 | Scaffold React/Vite/Tailwind frontend | NFR-002 | Platform | M | T-001 | Done |
| T-004 | Add Docker Compose and env examples | NFR-002 | Platform | M | T-002,T-003 | Done |
| T-005 | Implement owner registration/auth | REQ-AUTH-001, REQ-NFR-001 | Identity | L | T-002 | Done |
| T-006 | Implement pet profile CRUD | REQ-PET-001 | Pet | M | T-005 | Done |
| T-007 | Implement activity recording/history | REQ-ACTIVITY-001 | Activity | M | T-006 | Done |
| T-008 | Implement dashboard summary | REQ-DASH-001 | Activity | M | T-007 | Done |
| T-009 | Build frontend auth/pet/activity/dashboard screens | REQ-AUTH-001, REQ-PET-001, REQ-ACTIVITY-001, REQ-DASH-001 | Web | L | T-005,T-006,T-007,T-008 | Done |
| T-010 | Implement AI insight port and fallback | REQ-AI-001 | AI Insights | M | T-008 | Done |
| T-011 | Add optional Spring AI provider adapter | REQ-AI-001 | AI Insights | M | T-010 | Planned |
| T-012 | Implement goals/streaks/badges | REQ-GAME-001 | Engagement | L | T-008 | Planned |
| T-013 | Implement challenges/rewards | REQ-CHAL-001, REQ-REWARD-001 | Engagement | L | T-012 | Planned |
| T-014 | Implement partner campaigns/analytics | REQ-PARTNER-001 | Partner | L | T-013 | Partial — campaigns + AI topic analysis done; analytics in T-023 |
| T-015 | Add Testcontainers/API/security/E2E coverage | REQ-NFR-001, REQ-NFR-002 | Platform | L | T-005 onward | Planned |
| T-016 | Pricing configuration properties (CPP, campaign price, campaign credit cost) with startup validation | REQ-CONF-001 | Platform | S | T-002 | Planned |
| T-017 | Ledger module: wallet + `MAIN`/`BONUS`/`PARTNER` accounts on partner registration, append-only entries, atomic credit/debit with `SELECT … FOR UPDATE` + `@Version`, `V4` migration | REQ-LEDGER-001, REQ-LEDGER-002, REQ-NFR-003 | Ledger | L | T-016 | Planned |
| T-018 | Billing module: payment transactions, `PaymentGatewayPort`, fake provider, AbacatePay hosted checkout adapter, purchase API | REQ-BILL-001 | Billing | L | T-017 | Planned |
| T-019 | AbacatePay webhook (secret + HMAC, dedup), state machine incl. dispute/refund → block partner, credit-on-confirm, pending expiry job | REQ-BILL-002, REQ-NFR-003 | Billing/Ledger | L | T-018 | Planned |
| T-020 | Campaign creation debits credits `MAIN`→`BONUS`→`PARTNER` (same transaction, `402`, `423` when blocked, `Idempotency-Key`) | REQ-CAMP-003 | Partner/Ledger | M | T-017 | Planned |
| T-021 | Partner portal wallet: buy credits, balances, statement, payment history | REQ-BILL-001, REQ-LEDGER-003 | Partner portal | M | T-018,T-020 | Planned |
| T-022 | Campaign click API + web/mobile click instrumentation | REQ-CAMP-001 | Partner/Web/Mobile | M | T-014 | Planned |
| T-023 | Partner performance dashboard API + UI (clicks, unique owners, credits spent, cost per click) | REQ-CAMP-002 | Partner portal | L | T-022,T-020 | Planned |
| T-024 | Partner public profile: categories, services, website, address, automatic geocoding, PostGIS location | REQ-PARTNER-002 | Partner | M | T-014 | Planned |
| T-025 | Owner partner detail page (web + mobile) opened from campaign click | REQ-DISC-001 | Web/Mobile | M | T-022,T-024 | Planned |
| T-026 | Partner discovery map + active campaign list (web + mobile, MapLibre/OSM) | REQ-DISC-002, REQ-NFR-004 | Partner/Web/Mobile | L | T-024 | Planned |
| T-027 | Activity routes: GeoJSON `activity_routes`, API route field, route map from GeoJSON on web/mobile | REQ-TRACK-002, REQ-NFR-004 | Activity | M | T-007 | Planned |
| T-028 | Mobile Home quick-start live tracking (background task, local GeoJSON store, offline queue, Strava connect prompt) | REQ-TRACK-001 | Mobile | L | T-027 | Planned |
| T-029 | Enable transactional outbox publisher | ADR-0006 | Platform | M | T-002 | Planned |
| T-030 | Strava OAuth connection, encrypted tokens, preferences | REQ-STRAVA-001 | Integration | M | T-005 | Planned |
| T-031 | Strava publication worker (GPX/manual, retry, status in activity) | REQ-STRAVA-002 | Integration | M | T-028,T-029,T-030 | Planned |
| T-032 | Concurrency, idempotency, and privacy test suite for credits/routes | REQ-NFR-003, REQ-NFR-004 | Platform | M | T-019,T-020,T-027 | Planned |
| T-033 | Update OpenAPI, `.env.example`, README, and run guide per CR-2026-11 slice | REQ-NFR-002 | Platform | S | each slice | Planned |
| T-034 | Campaign expiration job + archive with prorated refund | REQ-CAMP-004 | Partner/Ledger | M | T-020 | Planned |
| T-035 | Credits explainer + pricing endpoint in partner portal | REQ-LEDGER-004 | Partner portal | S | T-016 | Planned |
| T-036 | Admin/AI challenge and badge authoring, many-owner enrollment and awards | REQ-CHAL-002 | Engagement | L | T-011 | Planned |
| T-037 | Admin wallet unblock/adjustments and partner block handling | REQ-BILL-002, REQ-LEDGER-001 | Ledger/Admin | M | T-019 | Planned |
| T-038 | Account deletion with grace period and retention jobs | REQ-NFR-001 | Platform | M | T-005 | Planned |
