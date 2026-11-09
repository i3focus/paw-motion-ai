# Event Contracts

## Envelope

Every integration event uses the envelope in `event-envelope.schema.json`:

- `eventId`: unique event identifier for idempotency.
- `eventType`: namespaced past-tense fact, e.g. `ActivityRecorded.v1`.
- `eventVersion`: schema version.
- `occurredAt`: domain occurrence timestamp in UTC.
- `aggregateId`: UUID of the aggregate that produced the event.
- `correlationId`: request/trace correlation.
- `causationId`: optional UUID of the causing event/request.
- `payload`: versioned event-specific JSON object.

## Versioning

- Add optional payload fields within the same major version only.
- Breaking changes require a new `eventType`/`eventVersion`.
- Consumers must ignore unknown fields.

## Initial events

See `docs/design/domain-events.md` for producer/consumer mapping. `PetCreated.v1` and `ActivityRecorded.v1` are the initial planned integration events. The current implementation slice only persists Pet and Activity records; publication through the outbox/event port is scheduled for a later integration slice and is not enabled yet.
