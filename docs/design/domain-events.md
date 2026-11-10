# Domain Events

All integration events use the common envelope defined in `docs/api/events/event-envelope.schema.json`.

| Event | Producer | Consumers | Payload summary |
|---|---|---|---|
| `OwnerRegistered.v1` | Identity | Notification | ownerId, occurredAt |
| `PetCreated.v1` | Pet | Activity, Engagement | petId, ownerId, species, size |
| `PetDeactivated.v1` | Pet | Activity, Engagement | petId, ownerId |
| `ActivityRecorded.v1` | Activity | Engagement, AI Insights, Integration, Notification | activityId, petId, ownerId, type, startedAt, durationSeconds, distanceMeters, optional `source`, `hasRoute`, `publishToStrava` |
| `WeeklyGoalUpdated.v1` | Engagement | Notification | ownerId, weeklyGoalMinutes |
| `ChallengeCompleted.v1` | Engagement | Partner, Notification | challengeId, ownerId, rewardId |
| `RewardIssued.v1` | Engagement | Partner, Notification | rewardId, ownerId, couponId |
| `CouponRedeemed.v1` | Partner | Engagement, Notification | couponId, campaignId, ownerId, redeemedAt |
| `InsightGenerated.v1` | AI Insights | Notification | insightId, ownerId, provider, fallbackUsed |
| `PaymentConfirmed.v1` | Billing | Notification | transactionId, partnerId, amount, currency, occurredAt |
| `PaymentFailed.v1` | Billing | Notification | transactionId, partnerId, status, reason |
| `PaymentRefunded.v1` | Billing | Ledger (compensation), Notification | transactionId, partnerId, amount, reason (`REFUND`/`CHARGEBACK`) |
| `PaymentDisputed.v1` | Billing | Ledger (block wallet), Notification | transactionId, partnerId, status |
| `PartnerWalletBlocked.v1` | Ledger | Partner (pause campaigns), Notification | walletId, partnerId, reason |
| `CampaignArchived.v1` | Partner | Ledger (refund), analytics | campaignId, partnerId, creditsRefunded |
| `CampaignExpired.v1` | Partner | analytics, Notification | campaignId, partnerId, expiresAt |
| `ChallengePublished.v1` | Engagement | Notification | challengeId, createdBy (`ADMIN`/`AI`), badgeId |
| `BadgeAwarded.v1` | Engagement | Notification, Insights | badgeId, ownerId, challengeId |
| `CreditsPurchased.v1` | Ledger | Partner (analytics), Notification | walletId, accountId, partnerId, credits, balanceAfter, transactionId |
| `CampaignCreditsDebited.v1` | Ledger | Partner (analytics), Notification | walletId, accountId, partnerId, campaignId, credits, balanceAfter |
| `CampaignCreated.v1` | Partner | AI topic analysis, Notification | campaignId, partnerId, creditsCharged |
| `CampaignClickRecorded.v1` | Partner | Partner analytics projection | campaignId, partnerId, source, placement, occurredAt (no owner PII) |
| `StravaConnected.v1` / `StravaConnectionRevoked.v1` | Integration | Notification | ownerId, occurredAt |
| `ActivityPublishedToStrava.v1` | Integration | Notification | activityId, ownerId, stravaActivityId |
| `StravaPublicationFailed.v1` | Integration | Notification | activityId, ownerId, reason |

## Delivery rules

- Events are at-least-once; consumers must be idempotent by `eventId`.
- `correlationId` propagates request tracing; `causationId` links cause/effect.
- Payloads contain IDs and minimal business facts, not full aggregates.
- Schema evolution is backward compatible: add optional fields only within a major version.
- Money-related events (`CreditsPurchased.v1`, `CampaignCreditsDebited.v1`, `Payment*.v1`) are written to the outbox in the same transaction as the ledger posting/state change (ADR-0014, ADR-0015).
