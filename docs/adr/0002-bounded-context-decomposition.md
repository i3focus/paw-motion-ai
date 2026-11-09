# ADR-0002: Bounded-Context Decomposition

- **Status**: Proposed
- **Context**: The PRD defines identity, pet, activity, AI insights, engagement, partner, and notification capabilities.
- **Decision**: Model these as bounded contexts with explicit ownership, but deploy as modules in the monolith initially.
- **Alternatives**: One generic CRUD model; microservice per context immediately.
- **Rationale**: DDD boundaries clarify ownership and future extraction without premature distribution.
- **Positive consequences**: Clear data ownership and contracts.
- **Negative consequences**: More design discipline required.
- **Risks**: Shared entities could blur boundaries.
- **Implementation guidance**: Use context packages and context-owned tables; reference other contexts by IDs.
- **Migration/reversal**: Extract contexts when independent scaling or ownership demands it.
