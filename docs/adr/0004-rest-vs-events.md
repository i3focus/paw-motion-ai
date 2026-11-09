# ADR-0004: REST vs Asynchronous Events

- **Status**: Proposed
- **Context**: The system needs synchronous user operations and cross-context reactions.
- **Decision**: Use REST/OpenAPI for commands and queries; use domain events for cross-context facts.
- **Alternatives**: All synchronous service calls; all asynchronous.
- **Rationale**: REST is simple for UI workflows; events preserve loose coupling for progress, rewards, notifications, and AI.
- **Positive consequences**: Clear API contracts and independent consumers.
- **Negative consequences**: Eventual consistency for projections.
- **Risks**: Consumers must handle duplicates and ordering.
- **Implementation guidance**: Version events and make consumers idempotent.
- **Migration/reversal**: Add synchronous read APIs if UX requires immediate consistency.
