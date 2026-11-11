# Paw Motion AI

**Problem:** Pet owners struggle to maintain consistent physical activity routines with their pets. Existing fitness tools primarily focus on humans, leaving pet owners without a simple, engaging way to track shared activities, understand progress, and stay motivated.

**Solution:** PawMotion AI is an AI-powered pet wellness companion that transforms walks, runs, and playtime into measurable, personalized, and rewarding experiences. Through activity tracking, intelligent insights, and gamified challenges, we help pets and their owners build healthier habits together.

**Our mission:** Make every step count — for pets and the people who love them.

An intelligent activity tracker that helps pet owners record walks, understand exercise routines, and build healthier habits together with their pets.

Category: **Saúde & bem-estar** (Health & Wellness).

The regulations explicitly include physical activity and wellness projects. PawMotion AI fits because its main purpose is to encourage and track physical activity, rather than provide veterinary diagnosis.

## Features

- Pet profiles: species, age, size, health passport, reminders, badges.
- Social feed: stories, photo/activity/achievement/sponsored cards, typed reactions, comments.
- Activity tracking: walks, runs, duration, distance, routes — with weekly activity charts.
- AI Activity Coach: chat assistant with confirmation-gated action cards and weekly insights.
- Gamification: XP, levels, streaks, challenges, neighborhood leaderboard.
- Map: nearby parks/vets/cafes, playdates, lost-pet alerts and sightings.
- Communities and events.
- Partner portal: business profile, services & bookings, campaign lifecycle with admin review, credits & billing.
- Notifications: in-app plus push pipeline (Expo/EAS).

## Tech Stack

| Layer | Tech |
|---|---|
| Backend | Spring Boot 4 / Java 21 + Kotlin modules, PostgreSQL + PostGIS, Flyway, JWT |
| Web | React 19 + TypeScript + Vite, react-router, Tailwind (PETLIFE design tokens) |
| Mobile | Expo (expo-router file-based navigation), React Native |
| AI | OpenAI API (primary) with deterministic fallback |
| Shared | `packages/design-tokens`, `packages/api-client` (generated from OpenAPI) |

## Repository structure

- `docs/`: requirements, traceability, DDD design, ADRs, API/event contracts, refactor plan/report, and the [run-all-projects guide](docs/run-all-projects.md).
- `backend/`: Spring Boot modular monolith using hexagonal architecture per bounded context.
- `frontend/`: React web app — landing `/`, owner app `/app/*`, partner portal `/partner/*`, admin review `/app/admin`.
- `mobile/`: Expo app for pet owners.
- `packages/`: shared design tokens and generated API client.
- `docker-compose.yml`: local PostgreSQL/PostGIS (published on `127.0.0.1:5432`), backend, and frontend.

## Run

```bash
cp .env.example .env            # fill DB creds + JWT_SECRET
docker compose up -d postgres
cd backend && ./gradlew bootRun # http://localhost:8080 (health: /actuator/health)
cd frontend && npm run dev      # http://localhost:5173
cd mobile && npm run start      # Expo
```

## Test

```bash
cd backend && ./gradlew test    # unit + Testcontainers (needs Docker)
cd frontend && npm test         # vitest
cd mobile && npm test           # jest-expo
```

## Key docs

- `docs/refactor-report.md` — what shipped in the 2026-11 redesign refactor (phases 0–6)
- `docs/refactor-plan.md` — the refactor contract (complete; re-run playbook)
- `docs/validation-skills.md` — vendored audit skills + re-run instructions
- `docs/adr/` — architecture decision records
- `docs/api/openapi.yaml` — generated API contract (`cd backend && ./gradlew exportOpenapi`)
