# Run all PawMotion AI projects

This guide runs the full local platform: PostgreSQL/PostGIS, Spring Boot backend, owner + partner web portals, and the Expo mobile app.

## Ports

| Project | URL / port | Notes |
|---|---:|---|
| PostgreSQL | `localhost:5432` | Compose-only by default |
| Backend API | `http://localhost:8080` | `/actuator/health` should return `UP` |
| Pet owner web | `http://localhost:5173` | Owner portal |
| Partner web portal | `http://localhost:5173/partner` | Same frontend build, separate auth/session |
| Expo Metro | `http://localhost:8081` | Mobile dev server / QR code |

## 1. Prepare environment

From the repository root:

```bash
cp .env.example .env
```

Edit `.env` and set local-only values for `POSTGRES_PASSWORD` and `JWT_SECRET`. Keep `.env` out of Git.

## 2. Start backend, database, and web portals

```bash
./scripts/dev-up.sh
```

This is equivalent to building and starting the Compose services for `postgres`, `backend`, and `frontend`.

Verify:

```bash
curl http://localhost:8080/actuator/health
curl -I http://localhost:5173
curl -I http://localhost:5173/partner
```

Open:

- Pet owner portal: `http://localhost:5173`
- Partner portal: `http://localhost:5173/partner`

Both portals are served by the same frontend container. They use separate routes and separate browser storage keys.

## 3. Optional: run web frontends with Vite dev servers

Use this when you want hot reload instead of the production frontend container.

```bash
docker compose up -d postgres backend

cd frontend
npm install
npm run dev
```

The Vite dev server also runs on `http://localhost:5173` and proxies `/api` to `http://localhost:8080`. The partner portal is available at `http://localhost:5173/partner`.

If port `5173` is already used by Compose, stop the Compose frontend first:

```bash
docker compose stop frontend
```

## 4. Start the Expo mobile app

The mobile app is for pet owners only. Partners use the web Partner Portal.

```bash
cd mobile
npm install
npm start
```

Scan the QR code with Expo Go on a physical device, or press `a` for an Android emulator when one is available.

### Android emulator

If using the Android emulator, the default API URL is already `http://10.0.2.2:8080`. Make sure Metro is reachable from the emulator:

```bash
adb reverse tcp:8081 tcp:8081
```

If the app cannot reach the backend, restart Metro with a cleared cache:

```bash
CI=1 npx expo start --clear --port 8081
```

### Physical device or remote backend

Set the API base URL before starting Expo:

```bash
EXPO_PUBLIC_API_BASE_URL=http://<your-machine-LAN-IP>:8080 npm start
```

For Android emulators, prefer `http://10.0.2.2:8080`; for iOS simulators, `http://localhost:8080` usually works.

## 5. Smoke-test the platform

1. Owner portal: register a pet owner, create a pet, edit the pet, record an activity, generate an insight, and check recommendations.
2. Partner portal: register a partner, create a campaign, run AI analysis, and verify the campaign topics appear for owners under recommendations.
3. Mobile app: log in with the same owner account and confirm dashboard, pets, activities, insights, recommendations, and profile load.

## 6. Verification commands

```bash
cd backend
./gradlew test

cd ../frontend
npm install
npm run typecheck
npm run build

cd ../mobile
npm install
npm run typecheck
```

CI runs the equivalent backend/frontend/mobile checks on pushes and pull requests.

## Troubleshooting

- Backend unhealthy: check `docker compose logs backend` and confirm `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, and `JWT_SECRET` are set in `.env`.
- Frontend cannot reach API: confirm backend is healthy and the frontend is proxying `/api` to `http://backend:8080` inside Compose.
- Expo cannot load the bundle: run `adb reverse tcp:8081 tcp:8081` for Android emulators and restart Metro with `--clear`.
- Mobile cannot reach API: set `EXPO_PUBLIC_API_BASE_URL`; Android emulator uses `http://10.0.2.2:8080`, physical devices need the machine LAN IP.
- AsyncStorage errors in Expo Go: keep `@react-native-async-storage/async-storage` aligned with the Expo SDK version in `mobile/package.json`.
