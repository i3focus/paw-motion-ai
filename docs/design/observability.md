# Observability

## Signals

- **Logs**: structured JSON logs with `traceId`, `spanId`, `correlationId`, module, use case, and outcome.
- **Metrics**: Micrometer metrics for HTTP latency, use-case counts, AI provider latency/errors/token usage, event publish/consume counts, coupon redemption conflicts.
- **Traces**: OpenTelemetry instrumentation for HTTP, JDBC, and messaging where supported.
- **Health**: Spring Boot Actuator health, readiness, and liveness endpoints.

## AI observability

Track provider, model, prompt version, latency, token usage, estimated cost, validation result, fallback reason, and correlation ID. Never log prompt contents containing personal data unless explicitly redacted.

## Alerting assumptions

- Alert on sustained 5xx rate, database connectivity failure, AI provider circuit-open state, and event consumer lag.
- Dashboard demo should expose health and key business counters.

## Local demo

Local Compose may expose Actuator and a simple metrics endpoint, but external observability backends are not required for the hackathon MVP.
