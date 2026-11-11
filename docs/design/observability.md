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

## As-built (post-refactor, 2026-11)

- `GET /actuator/health` exposes liveness/readiness groups (verified UP under
  compose postgres).
- Structured logging via logback; request flow is traceable by controller/service.
- Live-diagnosis tooling is vendored: `jstall` and JFR analysis skills under
  `.devin/skills/` — see `docs/validation-skills.md` for the triage workflow and
  last healthy-run results (0 lock contention, 7 ms GC, BCrypt as the only hotspot).
- No metrics/tracing stack is deployed for the MVP; actuator is the entry point for
  adding one.
