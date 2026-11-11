# Context Map

```mermaid
flowchart LR
    Identity -- "OwnerId, roles" --> Pet
    Identity -- "OwnerId, roles" --> Activity
    Pet -- "PetId" --> Activity
    Activity -- "ActivityStatistics" --> AIInsights[AI Insights]
    Activity -- "ActivityRecorded" --> Engagement
    Engagement -- "RewardIssued" --> Partner
    Engagement -- "NotificationRequested" --> Notification
    Partner -- "Campaign analytics" --> PartnerPortal[Partner Portal]
    Billing -- "credit(paymentId) in same tx" --> Ledger
    Partner -- "debit(campaignId) in same tx" --> Ledger
    Billing -- "PaymentGatewayPort" --> PSP[(Payment provider)]
    OwnerApps[Owner web/mobile] -- "Campaign interactions" --> Partner
    Activity -- "ActivityRecorded (route, publish flag)" --> Integration
    Integration -- "StravaPort" --> Strava[(Strava API)]
```

## Relationships

- **Identity → Pet/Activity**: shared kernel of owner identity; contexts reference `OwnerId` only.
- **Pet → Activity**: conformist on pet identity; Activity validates pet ownership through Pet port.
- **Activity → AI Insights**: customer/supplier; AI consumes read-only aggregates.
- **Activity → Engagement**: event-driven; Activity publishes facts, Engagement evaluates rules.
- **Engagement → Partner**: partner consumes reward/campaign contracts; coupon inventory remains Partner-owned.
- **Engagement → Notification**: event-driven notification requests.
- **Billing → Ledger / Partner → Ledger**: customer/supplier through the Ledger inbound port (`PostLedgerEntryUseCase`), executed in the caller's local transaction (ADR-0014).
- **Billing → Payment provider**: anti-corruption layer `PaymentGatewayPort` (ADR-0015).
- **Activity → Integration**: event-driven; Integration publishes to Strava asynchronously (ADR-0018).
- **Owner apps → Partner**: click tracking API feeding aggregate analytics (ADR-0016).

## Anti-corruption notes

- External AI providers are isolated behind `AiInsightProviderPort`.
- Strava is isolated behind `StravaPort`; payment providers behind `PaymentGatewayPort`; geocoding behind `GeocodingPort`; map tiles are configuration only.
- Frontend DTOs are API contracts, not domain models.

## As-built relationships (post-refactor, 2026-11)

- `feed` composes items from `content` (posts), `activity`, `engagement`
  (achievements), and `ads` (sponsored cards) — read-time fan-out into typed
  `feed_items`.
- `ads` → `ledger` (campaign debit, prorated `CAMPAIGN_REFUND` on end) and →
  `booking` (BOOKING CTAs bind a partner service).
- `booking` → `ads` (optional `campaign_id` attribution) and → `notification`
  (owner + partner notices).
- `assistant` executes confirmed actions through other contexts' use cases
  (confirmation-gated).
- `map` consumes `partner_profiles` geolocation (ADR-0021) for discovery.
- `identity` owns auth + role checks consumed by every context via security config.
