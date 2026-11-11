# Validation skills

Six community skills are vendored under `.devin/skills/` and were used to audit the
codebase after the six-phase refactor. This document records what each checks, how to
re-run it, and the last results.

| Skill | Source | Scope |
|---|---|---|
| `api-idempotency-keys` | `Intense-Visions/harness-engineering` | `Idempotency-Key` contract on mutation endpoints |
| `hibernate-jpa-validator` | `adityamparikh/hibernate-jpa-validator-skill` | Entity mapping, fetch plans, N+1, locking, pool config |
| `java-best-practices` | `stephanj/claude-code-collections` | Records, sealed types, error handling, functional patterns |
| `java-architect` | `stephanj/claude-code-collections` | Spring Boot architecture, security, transactions |
| `jfr-analyzer` | `victorrentea/performance-profiling` | JFR recording analysis (`jfr` CLI) |
| `jstall` | `parttimenerd/jstall` | Live JVM diagnosis (threads, locks, GC, metaspace) |

## How to re-run each validation

### api-idempotency-keys (static audit)
1. List mutation endpoints: `grep -rhn "@PostMapping\|@PatchMapping\|@DeleteMapping\|@PutMapping" backend/src/main`
2. List endpoints accepting the header: `grep -rn 'Idempotency-Key' backend/src/main`
3. For each covered endpoint, verify per the skill: replay returns the stored result (not re-execution), key reuse with a different body → 4xx, a uniqueness constraint backs the key, concurrent same-key requests can't double-execute.
4. Judge uncovered mutations by harm: duplicate creates with financial/ledger side effects need keys; social creates (posts/comments) are acceptable without them.

### hibernate-jpa-validator (static audit)
Run the entity checklist in `.devin/skills/hibernate-jpa-validator/SKILL.md` section B
against every `@Entity` (`grep -rln "@Entity" backend/src/main/java`): identifier
strategy, equals/hashCode, associations + fetch types, cascades, `@Version`,
`@DynamicUpdate`, column constraints, Lombok usage. Then check `application.yml`:
`ddl-auto` must be `validate` (Flyway owns schema), `open-in-view: false`.

### java-best-practices / java-architect (static audit)
- `grep -rn "catch (Exception\|catch (Throwable" backend/src/main` — every catch must wrap with cause, be a documented fallback, or sit at the outermost boundary.
- `grep -rn "@RequestBody" backend/src/main | grep -v "@Valid"` — unvalidated inputs (raw `byte[]` webhooks are exempt by design).
- Constructor injection (`final` fields), records for DTOs, no `System.getenv` scatter, `@Transactional` on all writes.

### jfr-analyzer (live recording)
1. Start the stack: `docker compose up -d postgres && cd backend && DB_URL=... ./gradlew bootRun`
2. Start a recording: `jcmd <app-pid> JFR.start name=val settings=profile`
3. Drive traffic (register + feed/discover/map calls), then dump: `jcmd <app-pid> JFR.dump name=val filename=/tmp/pawmotion.jfr`
4. Analyze per the skill: `jfr summary file.jfr`, then `jfr print --json file.jfr | python3 <aggregation script>` — check `JavaMonitorEnter` (contention), `ExecutionSample` (hot methods), `SocketRead/Write` (I/O), `GCPhasePause`, `JavaExceptionThrow`.

### jstall (live diagnosis)
With the app running (`bootRun` or container):
```
java -jar jstall.jar list                              # find the app JVM pid
java -jar jstall.jar status --intelligent-filter --no-native <pid>
```
Walk the triage tree in `.devin/skills/jstall/SKILL.md` (deadlock / high-CPU /
lock-contention / idle-slow / GC / classloader-leak). Binary:
`curl -LO https://github.com/parttimenerd/jstall/releases/latest/download/jstall.jar`

## Results — 2026-11-11

- **Idempotency**: `POST /api/partner/campaigns` and `POST /api/partner/billing/purchases` implement the full contract (replay → stored result, field-mismatch reuse → error, unique index `(partner_id, idempotency_key)`, concurrent retry → returns existing). Deviations (accepted, low): key reuse with a different body returns 400 rather than 422; keys persist on the resource row (no TTL needed — they are not a separate store); `POST /api/bookings`, `/api/posts`, `/api/feed/{id}/comments|reaction` have no key — duplicates cause UX noise, not financial harm.
- **JPA**: clean. UUID ids assigned in code, zero associations (no N+1 surface), no Lombok, `@Version` on every mutable entity (append-only `LedgerEntry`/`PaymentWebhookEvent` correctly skip it), `ddl-auto: validate`, `open-in-view: false`, Flyway-managed schema.
- **Java practices/architecture**: clean. Records for DTOs, constructor injection, domain exception hierarchy, boundary fallbacks preserve cause chains, `@Valid` on all typed request bodies, `@Transactional` on services, no swallowed exceptions.
- **JFR** (1s profile recording under register/feed/discover traffic): zero lock contention, zero exceptions/errors, GC total 7ms; only hot method `BCrypt.encipher` — intentional (password work factor).
- **jstall** (live): no deadlocks, no lock-dependency trees, 2.2% CPU, heap 50.8%, metaspace stable, no classloader growth. Nit: local JVM 21.0.8 is ~16 months old — update the dev JDK when convenient.
- **Fix applied**: `docker-compose.yml` now publishes postgres on `127.0.0.1:5432` (the docs' `DB_URL=jdbc:postgresql://localhost:5432` previously had no mapped port).
