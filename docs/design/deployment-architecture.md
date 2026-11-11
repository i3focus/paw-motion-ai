# Deployment Architecture

## Minimum viable topology

```mermaid
flowchart TB
    subgraph Local[Local development]
      FE[Vite dev server :5173] --> API[Spring Boot :8080]
      API --> PG[(Postgres + PostGIS container)]
      API -. optional .-> RMQ[RabbitMQ container]
    end
    subgraph Demo[Hackathon demo]
      Static[Static frontend hosting] --> API2[Spring Boot service]
      API2 --> PG2[(Managed PostgreSQL)]
    end
```

## Deployment units

| Unit | Technology | Responsibility |
|---|---|---|
| `frontend` | React + TypeScript + Vite + Tailwind | SPA and API client |
| `backend` | Java 21 + Spring Boot | Modular monolith API |
| `postgres` | PostgreSQL + PostGIS | System of record |
| `rabbitmq` | RabbitMQ | Optional asynchronous events |

## Environment strategy

- `local`: Docker Compose with Postgres/PostGIS and optional RabbitMQ.
- `demo`: same containers or managed equivalents; AI provider can be disabled.
- `prod`: HTTPS, managed PostgreSQL, secret manager, least-privilege DB user, backups.

## Scaling path

The monolith can scale vertically first. If activity ingestion or AI workloads justify it, extract Activity or AI Insights into services without changing domain contracts. Do not split Pet and Identity prematurely.

## Backup and recovery assumptions

- PostgreSQL automated backups for managed environments.
- Flyway migrations are forward-only after production release.
- Account deletion workflow must remove or anonymize owner-owned data according to retention policy.

## As-built (post-refactor, 2026-11)

`docker-compose.yml` runs `postgres` (postgis/postgis:17-3.5, published on
`127.0.0.1:5432` for host tools), `backend`, and `frontend`. Local backend dev runs
`./gradlew bootRun` against that postgres with env from `.env`. Mobile is developed
through Expo (`npm run start`) and talks to the same backend; it is not part of
compose. Production deployment is out of MVP scope; push notifications require an
EAS `projectId` (`app.json` → `extra.eas.projectId`).
