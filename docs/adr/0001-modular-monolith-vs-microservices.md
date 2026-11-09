# ADR-0001: Modular Monolith vs Microservices

- **Status**: Proposed
- **Context**: Hackathon MVP with a two-developer team and broad domain scope.
- **Decision**: Use one Spring Boot deployable organized as bounded-context modules.
- **Alternatives**: Separate microservices per bounded context; serverless functions.
- **Rationale**: Faster local development, simpler transactions, easier debugging, and lower operational cost. Boundaries remain enforceable through package structure and module-owned tables.
- **Positive consequences**: One build/deploy unit; simple Docker Compose; easier end-to-end demo.
- **Negative consequences**: Scaling and team boundaries are less isolated; accidental coupling is possible.
- **Risks**: Module boundaries may erode without architecture tests.
- **Implementation guidance**: Enforce package dependencies, no cross-module JPA relationships, and module-owned migrations.
- **Migration/reversal**: Extract Activity or AI Insights later if load or team size justifies it.
