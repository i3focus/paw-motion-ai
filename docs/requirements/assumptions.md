# Assumptions

1. The README MVP feature list is the minimum product scope for the hackathon.
2. The attached master prompt is the governing engineering process; the detailed product PRD is still pending.
3. A Spring Boot modular monolith is acceptable for the MVP and can be split later if a concrete scaling need appears.
4. PostgreSQL with PostGIS is the system of record; object storage can be added later for shareable cards.
5. A local OIDC provider is desirable, but the first local demo may use a development JWT issuer until Keycloak is added.
6. AI providers are replaceable. Local demo and CI use deterministic providers; paid providers are optional.
7. RabbitMQ is optional for the first vertical slice; domain events can initially be persisted and published through an adapter.
8. Frontend talks only to the backend API; it never accesses the database or AI providers directly.
9. CR-2026-11: partners prepay credits; campaign packages are no longer sold. Campaign creation is the only credit-consuming operation in the MVP.
10. Credits are integers, non-withdrawable, non-transferable between partners, and do not expire unless decided otherwise.
11. "CPP" means credits per one unit of fiat currency (BRL). Purchases grant `floor(amount × CPP)` credits.
12. Pricing values are environment variables read at startup; there is no admin UI for pricing in the MVP.
13. A deterministic fake payment provider is used locally and in CI; a real provider sandbox is used for the demo.
14. Owner apps record campaign clicks; partner analytics never expose individual owners.
15. Live tracking is a mobile-only feature; the web app shows tracked routes but does not track.
16. Strava publication is optional, opt-in, and asynchronous; the app works fully without Strava.
