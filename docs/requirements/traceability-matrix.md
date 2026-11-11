# Requirements Traceability Matrix

| Requirement | Priority | Bounded context | Design artifact | Planned code module | Test coverage |
|---|---:|---|---|---|---|
| REQ-AUTH-001 | Must | Identity | security-architecture.md, ADR-0008 | `identity` | unit + security tests |
| REQ-PET-001 | Must | Pet | domain-model.md, data-architecture.md | `pet` | domain + repository + API tests |
| REQ-ACTIVITY-001 | Must | Activity | domain-model.md, data-architecture.md | `activity` | domain + API + integration tests |
| REQ-DASH-001 | Must | Activity | software-architecture.md | `activity` dashboard use case | application + API tests |
| REQ-AI-001 | Must | AI Insights | domain-model.md, ADR-0009 | `aiinsights` | fallback + validation tests |
| REQ-GAME-001 | Should | Engagement | domain-model.md | `engagement` | domain tests |
| REQ-CHAL-001 | Should | Engagement | domain-events.md | `engagement` | application + concurrency tests |
| REQ-REWARD-001 | Should | Engagement/Partner | data-architecture.md | `engagement`, `partner` | concurrency tests |
| REQ-PARTNER-001 | Should | Partner | security-architecture.md | `partner` | authorization + aggregate tests |
| REQ-NFR-001 | Must | All | security-architecture.md | security config | security tests |
| REQ-NFR-002 | Must | Platform | deployment-architecture.md, observability.md | Docker/CI | smoke tests |
| REQ-BILL-001 | Must | Billing | domain-model.md, ADR-0015 | `billing` | application + idempotency tests |
| REQ-BILL-002 | Must | Billing/Ledger | domain-model.md, ADR-0014, ADR-0015 | `billing`, `ledger` | webhook + concurrency tests |
| REQ-LEDGER-001 | Must | Ledger | domain-model.md, data-architecture.md | `ledger` | domain + authorization tests |
| REQ-LEDGER-002 | Must | Ledger | ADR-0014 | `ledger` | concurrency (Testcontainers) + consistency tests |
| REQ-LEDGER-003 | Should | Ledger | data-architecture.md | `ledger` | API + pagination tests |
| REQ-CAMP-003 | Must | Partner/Ledger | domain-model.md, ADR-0014 | `partner`, `ledger` | transaction + idempotency tests |
| REQ-CONF-001 | Must | Platform | software-architecture.md | configuration | binding/validation tests |
| REQ-CAMP-001 | Must | Partner | ADR-0016 | `partner`, web, mobile | API + dedup tests |
| REQ-CAMP-002 | Must | Partner | ADR-0016, security-architecture.md | `partner`, partner portal | aggregation + no-PII tests |
| REQ-PARTNER-002 | Must | Partner | ADR-0017, data-architecture.md | `partner`, partner portal | validation + authorization tests |
| REQ-DISC-001 | Must | Partner | ADR-0017 | `partner`, web, mobile | API exposure + UI tests |
| REQ-DISC-002 | Should | Partner | ADR-0017 | `partner`, web, mobile | spatial query + UI tests |
| REQ-TRACK-001 | Must | Activity | ADR-0018 | `activity`, mobile | state machine + E2E tests |
| REQ-TRACK-002 | Must | Activity | ADR-0018, data-architecture.md | `activity`, web, mobile | persistence + authorization tests |
| REQ-STRAVA-001 | Should | Integration | ADR-0018, security-architecture.md | `integration` | OAuth + token tests |
| REQ-STRAVA-002 | Should | Integration | ADR-0018, ADR-0006 | `integration` | outbox/retry + idempotency tests |
| REQ-NFR-003 | Must | Billing/Ledger | ADR-0014, ADR-0015 | `billing`, `ledger` | concurrency + audit tests |
| REQ-NFR-004 | Must | Activity/Partner | security-architecture.md | `activity`, `partner` | security tests |
| REQ-CAMP-004 | Must | Partner, Ledger | ADR-0014 | `partner`, `ledger`, partner portal | expiration job, prorated refund, double-archive tests |
| REQ-LEDGER-004 | Must | Partner portal | ADR-0014 | partner portal | explainer renders config values |
| REQ-CHAL-002 | Should | Engagement | ADR-0009 | `engagement`, admin | admin/AI authoring, many-owner award idempotency |

## Delivered-state traceability (post-refactor, 2026-11)

| Area | Code module | Tests |
|---|---|---|
| Auth (owner + partner) | `identity` (`AuthController`, `PartnerAuthController`, JWT filter) | backend IT + unit |
| Pets & health | `pet`, `health`, `petprofile` | backend tests |
| Activities & feed | `activity`, `feed`, `content`, `engagement` | backend IT (`LegacyDataMigrationIT` proves V6→V19 loss-free) |
| Map & lost pets | `map` (places, playdates, sightings) | backend tests |
| AI assistant | `assistant`, `aiinsights` | backend tests |
| Communities | `community` | backend tests |
| Bookings | `booking`, `ads` (service binding) | `AdsApiIT` |
| Campaigns v2 + admin | `ads`, `partner`, `ledger` | `AdsApiIT`, domain tests |
| Billing/payments | `billing`, `ledger` | backend IT |
| Notifications | `notification` | backend tests |
| Web UI | `frontend/src/pages`, `src/partner` | vitest (`validation`, `PartnerCampaignForm`) + `tsc --noUnusedLocals` |
| Mobile | `mobile/app` (expo-router) | jest-expo (`client`, `validation`) |
| API contract | `docs/api/openapi.yaml` → `packages/api-client` | generated from live app |
