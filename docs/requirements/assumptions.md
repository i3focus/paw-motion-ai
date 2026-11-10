# Assumptions

1. The README MVP feature list is the minimum product scope for the hackathon.
2. The attached master prompt is the governing engineering process; the detailed product PRD is still pending.
3. A Spring Boot modular monolith is acceptable for the MVP and can be split later if a concrete scaling need appears.
4. PostgreSQL with PostGIS is the system of record; object storage can be added later for shareable cards.
5. The hackathon uses the platform's own JWT issuer; an external OIDC provider (Keycloak) is a post-hackathon option.
6. AI providers are replaceable. OpenAI is the primary demo provider; CI uses deterministic providers.
7. RabbitMQ is optional for the first vertical slice; domain events can initially be persisted and published through an adapter.
8. Frontend talks only to the backend API; it never accesses the database or AI providers directly.
9. CR-2026-11: partners prepay credits; campaign packages are no longer sold. Campaign creation is the only credit-consuming operation in the MVP.
10. Credits are integers, non-withdrawable, non-transferable between partners, and do not expire unless decided otherwise.
11. "CPP" means credits per one unit of fiat currency (BRL). Purchases grant `floor(amount × CPP)` credits.
12. Pricing values are environment variables read at startup; there is no admin UI for pricing in the MVP.
13. A deterministic fake payment provider is used in CI; AbacatePay Modo Teste (sandbox) is used for local demos with a public HTTPS tunnel for webhooks.
14. Owner apps record campaign clicks only (no impressions); partner analytics never expose individual owners.
15. Live tracking (including background) is a mobile-only feature; routes are GeoJSON; the web app shows tracked routes but does not track.
16. Strava publication is optional and asynchronous; the app prompts owners to connect Strava but works fully without it.
17. Docker Compose is the primary deployment target.
18. Purchases are in whole BRL amounts (AbacatePay checkout with a 1-BRL product and `quantity = amount`).
