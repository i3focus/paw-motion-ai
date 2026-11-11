# PETLIFE (paw-motion-ai)

AI-powered pet wellness platform: social feed, pet profiles, maps & lost-pet alerts, AI assistant, gamification, communities, partner campaigns, and service bookings.

## Stack

| Layer | Tech |
|---|---|
| Backend | Spring Boot 4 / Java 21 + Kotlin modules, PostgreSQL + PostGIS, Flyway, JWT |
| Web | React + TypeScript + Vite, react-router, Tailwind (PETLIFE design tokens) |
| Mobile | Expo (expo-router file-based navigation), React Native |
| Shared | `packages/design-tokens`, `packages/api-client` (generated from OpenAPI) |

## Run

```bash
cp .env.example .env            # fill DB creds + JWT_SECRET
docker compose up -d postgres   # published on 127.0.0.1:5432
cd backend && ./gradlew bootRun # http://localhost:8080 (health: /actuator/health)
cd frontend && npm run dev      # http://localhost:5173
cd mobile && npm run start      # Expo
```

Full instructions: `docs/run-all-projects.md`.

## Test

```bash
cd backend && ./gradlew test    # unit + Testcontainers (needs Docker)
cd frontend && npm test         # vitest
cd mobile && npm test           # jest-expo
```

## Docs

- `docs/refactor-report.md` — what shipped in the 2026-11 redesign refactor (phases 0–6)
- `docs/refactor-plan.md` — the refactor contract (now complete; re-run playbook)
- `docs/validation-skills.md` — vendored audit skills + re-run instructions
- `docs/design/` — architecture, data model, security, observability (each has an **As-built** section reflecting shipped state)
- `docs/adr/` — architecture decision records
- `docs/requirements/` — requirements + traceability
- `docs/api/openapi.yaml` — generated API contract (`cd backend && ./gradlew exportOpenapi` to regenerate)

## Re-running audits or the refactor

Validation skills live in `.devin/skills/` — see `docs/validation-skills.md` for the
checklists, live-diagnosis steps (jstall/JFR), and last results.
