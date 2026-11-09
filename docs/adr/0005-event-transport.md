# ADR-0005: Event Transport Selection

- **Status**: Proposed
- **Context**: PRD allows RabbitMQ or an interchangeable adapter; MVP must run locally without paid services.
- **Decision**: Define a messaging port and use RabbitMQ when enabled; provide an in-process/logging adapter for local demo and tests.
- **Alternatives**: Kafka; database polling only; no events initially.
- **Rationale**: RabbitMQ is sufficient for MVP event volume and familiar to Spring teams; the port preserves replaceability.
- **Positive consequences**: Optional infrastructure; testable consumers.
- **Negative consequences**: Additional operational component when enabled.
- **Risks**: Dual adapters could diverge.
- **Implementation guidance**: Contract-test both adapters and keep RabbitMQ optional in Compose.
- **Migration/reversal**: Replace RabbitMQ with Kafka or cloud events only if volume/replay needs justify it.
