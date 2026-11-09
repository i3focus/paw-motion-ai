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

- `docs/`: requirements, traceability, DDD design, ADRs, API/event contracts, backlog, implementation plan, and the [run-all-projects guide](docs/run-all-projects.md).
- `backend/`: Spring Boot 4 modular monolith using hexagonal architecture per bounded context.
- `frontend/`: React 19 + TypeScript + Vite + Tailwind CSS pet-owner web app and separate partner web portal under `/partner`.
- `mobile/`: Expo React Native app for pet owners only.
- `docker-compose.yml`: local PostgreSQL/PostGIS, backend, and frontend stack.

## Current implementation slice

Implemented platform slice:

- Separate owner and partner account roles sharing the same identity store and JWT issuer.
- Owner registration/login, profile management, pet profiles with add/edit flows, activity tracking, dashboard summary, AI insights, and AI-analyzed partner recommendations.
- Validated web/mobile inputs for email, password, phone, URLs, dates, and numeric activity fields, plus warm PawMotion visual cards using local dog imagery.
- Partner registration/login at `/partner`, profile management, campaign creation, and AI campaign topic analysis.
- Expo mobile app for pet owners covering auth, dashboard, pets, activities, insights, recommendations, and profile.
- OpenAPI contract and event schema under `docs/api/`.

Challenges, rewards, partner analytics dashboards, GPS routes, and external AI provider adapters are planned in the backlog and are not part of the current runnable slice.

## Local development

Prerequisites: Docker with Compose.

```bash
cp .env.example .env
# Set POSTGRES_PASSWORD and JWT_SECRET to random local values in .env
./scripts/dev-up.sh
```

Open `http://localhost:5173`. The frontend is served by Nginx and proxies `/api` to the backend container. For the complete owner/partner/mobile runbook, use [docs/run-all-projects.md](docs/run-all-projects.md).

For local frontend development against a locally running backend:

```bash
docker compose up postgres backend
cd frontend
npm install
npm run dev
```

The Vite dev server runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`. PostgreSQL is only reachable inside the Compose network; use `docker compose exec postgres psql -U pawmotion -d pawmotion` for direct database access.

## Verification

```bash
cd backend
./gradlew build

cd ../frontend
npm install
npm run typecheck
npm run build

cd ../mobile
npm install
npm run typecheck
```

CI runs backend Gradle build/tests, frontend install/build, and mobile install/typecheck on pushes and pull requests. Mobile defaults to `http://localhost:8080` on iOS/web and `http://10.0.2.2:8080` on Android emulators; set `EXPO_PUBLIC_API_BASE_URL` for physical devices or remote backends.

## Architecture notes

The backend is intentionally a Spring Boot modular monolith, not microservices. Each bounded context keeps domain models Spring-free, application use cases behind ports, and adapters for REST, persistence, and external providers. Client forms use shared validation helpers while the backend remains the authoritative validation boundary. See `docs/design/software-architecture.md`, `docs/adr/`, and `docs/run-all-projects.md` for the decisions and local runbook behind this structure.

When deploying behind an HTTPS proxy or proxy chain, set `TRUSTED_PROXY_CIDRS` for the frontend Nginx container and `TRUSTED_PROXIES` for the backend to the explicit proxy IPs/CIDRs you trust. Nginx only rewrites the client address for configured upstreams, and the backend only trusts forwarding headers from configured proxies.
