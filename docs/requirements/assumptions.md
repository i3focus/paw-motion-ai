# Assumptions

1. The README MVP feature list is the minimum product scope for the hackathon.
2. The attached master prompt is the governing engineering process; the detailed product PRD is still pending.
3. A Spring Boot modular monolith is acceptable for the MVP and can be split later if a concrete scaling need appears.
4. PostgreSQL with PostGIS is the system of record; object storage can be added later for shareable cards.
5. A local OIDC provider is desirable, but the first local demo may use a development JWT issuer until Keycloak is added.
6. AI providers are replaceable. Local demo and CI use deterministic providers; paid providers are optional.
7. RabbitMQ is optional for the first vertical slice; domain events can initially be persisted and published through an adapter.
8. Frontend talks only to the backend API; it never accesses the database or AI providers directly.
