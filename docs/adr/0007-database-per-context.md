# ADR-0007: Database-Per-Context Strategy

- **Status**: Proposed
- **Decision**: Each bounded context owns its tables inside one PostgreSQL database for the MVP. Cross-context foreign keys are forbidden.
- **Alternatives**: One shared schema; separate physical databases per context.
- **Rationale**: Logical ownership with operational simplicity. Separate databases add backup/migration overhead unsuitable for a hackathon.
- **Positive consequences**: Clear ownership, simpler local setup, future extraction path.
- **Negative consequences**: Cannot enforce ownership with database permissions alone.
- **Risks**: Accidental joins across contexts.
- **Implementation guidance**: Prefix or schema-separate tables by context; use Flyway migrations per module.
- **Migration/reversal**: Split into physical databases only when independent scaling or compliance requires it.
