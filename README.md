# Paw Motion AI

**Problem:** Pet owners struggle to maintain consistent physical activity routines with their pets. Existing fitness tools primarily focus on humans, leaving pet owners without a simple, engaging way to track shared activities, understand progress, and stay motivated.

**Solution:** PawMotion AI is an AI-powered pet wellness companion that transforms walks, runs, and playtime into measurable, personalized, and rewarding experiences. Through activity tracking, intelligent insights, and gamified challenges, we help pets and their owners build healthier habits together.

**Our mission:** Make every step count — for pets and the people who love them.

AI-powered activity companion for pets and their owners

An intelligent activity tracker that helps pet owners record walks, understand exercise routines, and build healthier habits together with their pets.

Category: **Saúde & bem-estar** (Health & Wellness).

The regulations explicitly include physical activity and wellness projects. PawMotion AI fits because its main purpose is to encourage and track physical activity, rather than provide veterinary diagnosis.

## MVP features
1. Pet profile: Name, species, age, size, and activity preferences.
2. Activity tracking: Record walks, runs, duration, distance, and frequency.
3. AI Activity Coach: Generate weekly activity summaries and personalized, non-medical suggestions.
4. Gamification: Walking streaks, badges, and weekly goals.
5. Shareable activity cards: Generate attractive summaries that users can share externally.

### Tech Stack
1. Frontend	React + TypeScript + Tailwind CSS
2. Backend	Java 21 + Spring Boot
3. Database	PostgreSQL + PostGIS
4. AI	Gemini API or OpenAI API
5. Maps	MapLibre + OpenStreetMap
6. Deployment	Vercel + Render/Fly.io
7. Optional integration: Strava API

## Repository structure

- `docs/`: requirements, traceability, DDD design, ADRs, API/event contracts, backlog, and implementation plan.
- `backend/`: Spring Boot 4 modular monolith using hexagonal architecture per bounded context.
- `frontend/`: React 19 + TypeScript + Vite + Tailwind CSS application.
- `docker-compose.yml`: local PostgreSQL/PostGIS, backend, and frontend stack.

## Current implementation slice

Implemented foundation for the first vertical slice:

- Owner registration and JWT login.
- Pet profile creation and listing.
- Activity recording and history.
- Dashboard summary with recent activities and streak.
- AI insight endpoint with deterministic local fallback and safety validation.
- OpenAPI contract and event schema under `docs/api/`.

Challenges, rewards, partner analytics, GPS routes, and external AI provider adapters are planned in the backlog and are not part of the first runnable slice.

## Local development

Prerequisites: Docker with Compose.

```bash
cp .env.example .env
# Set JWT_SECRET to a random local value in .env
./scripts/dev-up.sh
```

Open `http://localhost:5173`. The frontend is served by Nginx and proxies `/api` to the backend container.

For local frontend development against a locally running backend:

```bash
docker compose up postgres backend
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`.

## Verification

```bash
cd backend
./gradlew build

cd ../frontend
npm install
npm run build
```

CI runs backend Gradle build/tests and frontend install/build on pushes and pull requests.

## Architecture notes

The backend is intentionally a Spring Boot modular monolith, not microservices. Each bounded context keeps domain models Spring-free, application use cases behind ports, and adapters for REST, persistence, and external providers. See `docs/design/software-architecture.md` and `docs/adr/` for the decisions behind this structure.
