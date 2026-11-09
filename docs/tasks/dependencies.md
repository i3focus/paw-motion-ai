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
```

## Critical path

Auth → Pet → Activity → Dashboard → Frontend integration. AI, gamification, and partner work should not block this path.

## External dependencies

- PostgreSQL + PostGIS container image.
- Optional RabbitMQ container image.
- Optional AI provider credentials.
- Optional OIDC provider for production-like auth.
