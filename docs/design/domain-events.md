# Domain Events

All integration events use the common envelope defined in `docs/api/events/event-envelope.schema.json`.

| Event | Producer | Consumers | Payload summary |
|---|---|---|---|
| `OwnerRegistered.v1` | Identity | Notification | ownerId, occurredAt |
| `PetCreated.v1` | Pet | Activity, Engagement | petId, ownerId, species, size |
| `PetDeactivated.v1` | Pet | Activity, Engagement | petId, ownerId |
| `ActivityRecorded.v1` | Activity | Engagement, AI Insights, Notification | activityId, petId, ownerId, type, startedAt, durationSeconds, distanceMeters |
| `WeeklyGoalUpdated.v1` | Engagement | Notification | ownerId, weeklyGoalMinutes |
| `ChallengeCompleted.v1` | Engagement | Partner, Notification | challengeId, ownerId, rewardId |
| `RewardIssued.v1` | Engagement | Partner, Notification | rewardId, ownerId, couponId |
| `CouponRedeemed.v1` | Partner | Engagement, Notification | couponId, campaignId, ownerId, redeemedAt |
| `InsightGenerated.v1` | AI Insights | Notification | insightId, ownerId, provider, fallbackUsed |

## Delivery rules

- Events are at-least-once; consumers must be idempotent by `eventId`.
- `correlationId` propagates request tracing; `causationId` links cause/effect.
- Payloads contain IDs and minimal business facts, not full aggregates.
- Schema evolution is backward compatible: add optional fields only within a major version.
