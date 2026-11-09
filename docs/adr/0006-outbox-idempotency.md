# ADR-0006: Transactional Outbox and Idempotency

- **Status**: Proposed
- **Context**: Cross-context events must not be lost after database commits.
- **Decision**: Persist outbox records in the same transaction as state changes; publish asynchronously with retries. Consumers deduplicate by `eventId`.
- **Alternatives**: Publish directly after commit; exactly-once messaging.
- **Rationale**: Outbox provides reliable publication without distributed transactions; idempotency handles retries.
- **Positive consequences**: Consistent state and event delivery.
- **Negative consequences**: Extra table and relay process.
- **Risks**: Relay lag or duplicate processing if consumers are not idempotent.
- **Implementation guidance**: Add outbox/inbox tables before enabling RabbitMQ consumers.
- **Migration/reversal**: Can disable messaging and use synchronous adapters for demo if needed.
