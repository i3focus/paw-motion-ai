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

## As-built (post-refactor, 2026-11, updated for the security remediation)

- `GET /actuator/health` exposes liveness/readiness groups (verified UP under
  compose postgres). Everything else under `/actuator/**` (incl. `info` and
  `metrics`) now requires the ADMIN role (N4).
- Structured logging via logback; request flow is traceable by controller/service.
  E-mails are masked in logs (`LogSafe.email`) and private free text is not logged.
- Micrometer counters emitted by the application (scrape via
  `/actuator/metrics` as ADMIN):
  - `ratelimit.rejected{bucket,route}` — enforced rate-limit rejections (429);
    `ratelimit.shadow_violations{bucket,route}` — webhook-bucket violations while
    `RATE_LIMIT_MODE=shadow` (observe before promoting the bucket to `enforce`).
  - `payment.webhook.events{provider,result}` — payment webhook outcomes.
  - `ledger.credits{direction,reference}` — credit/debit amounts posted to wallets.
  - `notifications.inserted{type}` — in-app notifications persisted.
  - `media.files_deleted` — files removed by the media deletion outbox drain.
- Live-diagnosis tooling is vendored: `jstall` and JFR analysis skills under
  `.devin/skills/` — see `docs/validation-skills.md` for the triage workflow and
  last healthy-run results (0 lock contention, 7 ms GC, BCrypt as the only hotspot).
- No external metrics/tracing backend is deployed for the MVP; the actuator
  endpoints above are the entry point for adding one.
