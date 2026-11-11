# ADR 0024 — Idempotency-Key on financial and non-idempotent creates

Status: accepted (2026-11-11, validation pass)

## Context

Client retries (timeouts, double-taps) can duplicate creates. For operations that
move credits or create paid artifacts, duplicates mean real money errors.

## Decision

`POST /api/partner/campaigns` and `POST /api/partner/billing/purchases` accept a
client-generated `Idempotency-Key` (UUID v4). Semantics:

- Same key + same normalized payload → returns the stored result, no re-execution.
- Same key + different payload → 400 (documented deviation from the 422 convention).
- Unique index `(partner_id, idempotency_key)` enforces one result per key; a
  concurrent same-key race resolves to the existing row.
- Keys persist on the resource row — no separate key store, no TTL needed.

Social mutations (`posts`, `comments`, `reaction`, `bookings`) intentionally carry
no key: duplicates are UX noise, not financial harm. If any gains a side effect
that costs credits, it must adopt this contract.

## Consequences

- Retried campaign/purchase requests are safe end to end.
- The contract and last audit results are documented in `docs/validation-skills.md`.
