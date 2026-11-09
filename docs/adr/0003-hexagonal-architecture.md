# ADR-0003: Hexagonal Architecture

- **Status**: Proposed
- **Context**: The PRD mandates hexagonal architecture and replaceable infrastructure.
- **Decision**: Each module has domain, application ports/use cases, and inbound/outbound adapters.
- **Alternatives**: Layered MVC only; clean architecture with too many abstractions.
- **Rationale**: Domain logic remains independent of Spring/JPA/HTTP, improving testability and provider replaceability.
- **Positive consequences**: Clear ports for repositories, AI, messaging; easier tests.
- **Negative consequences**: More files and mapping code.
- **Risks**: Over-abstraction for simple CRUD.
- **Implementation guidance**: Keep domain Spring-free; controllers only adapt HTTP; repositories implement ports.
- **Migration/reversal**: Simplify adapters if complexity outweighs benefit.
