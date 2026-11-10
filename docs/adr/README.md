# Architecture Decision Records

The following ADRs record the initial architecture baseline. They are proposed pending team review.

| ADR | Decision |
|---|---|
| [0001](0001-modular-monolith-vs-microservices.md) | Modular monolith instead of microservices |
| [0002](0002-bounded-context-decomposition.md) | Bounded-context decomposition |
| [0003](0003-hexagonal-architecture.md) | Hexagonal architecture inside modules |
| [0004](0004-rest-vs-events.md) | REST for commands/queries, events for cross-context facts |
| [0005](0005-event-transport.md) | RabbitMQ behind an interchangeable port |
| [0006](0006-outbox-idempotency.md) | Transactional outbox and idempotent consumers |
| [0007](0007-database-per-context.md) | Database-per-context tables in one PostgreSQL database |
| [0008](0008-identity-provider.md) | OIDC target with development JWT fallback |
| [0009](0009-ai-provider-abstraction.md) | AI provider abstraction and deterministic fallback |
| [0010](0010-frontend-architecture.md) | React SPA with typed API client |
| [0011](0011-testing-strategy.md) | Unit, integration, contract, security, E2E tests |
| [0012](0012-deployment-operational-simplicity.md) | Docker Compose and simple deployment |
| [0013](0013-client-input-validation-and-pet-editing.md) | Client input validation and pet profile editing |
| [0014](0014-credit-ledger-and-wallets.md) | Credit ledger, wallets, and atomic campaign debit |
| [0015](0015-payment-gateway-abstraction.md) | Payment gateway abstraction and payment transaction lifecycle |
| [0016](0016-campaign-interaction-analytics.md) | Campaign interaction tracking and aggregate partner analytics |
| [0017](0017-partner-geolocation-and-discovery-maps.md) | Partner geolocation and discovery maps |
| [0018](0018-live-tracking-and-strava-publication.md) | Live GPS tracking and Strava publication |
