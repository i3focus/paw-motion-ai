# ADR-0016: Campaign Click Tracking and Aggregate Partner Analytics

- **Status**: Proposed
- **Context**: Partners need to see how many owners viewed (clicked) their campaigns to perceive value and keep buying credits. Decision (2026-11-10): track **clicks only** for now; impressions are out of scope. Partner analytics must stay aggregate-only (REQ-NFR-001, LGPD).
- **Decision**: Owner clients post a click to `POST /api/campaigns/{campaignId}/clicks` (source, placement) when the owner opens a campaign. The Partner context stores raw clicks in `campaign_clicks` (owner id kept only as a salted hash for unique counting and deduplication) and upserts a daily aggregate `campaign_daily_metrics (clicks, unique_clicks)` in the same transaction. Partner dashboard endpoints read only aggregates and ledger debits (credits spent, cost per click).
- **Alternatives**: Impressions + CTR (deferred); third-party analytics SDK (rejected: data leaves platform); computing aggregates per request from raw data (acceptable for tiny volumes, rejected for predictable latency); separate analytics database (premature).
- **Rationale**: Simple, in-database, privacy-preserving, and sufficient for MVP volumes; clicks are the clearest value signal.
- **Positive consequences**: Near real-time click counters linked to credits spent.
- **Negative consequences**: No reach/CTR metric until impressions are added.
- **Risks**: Click inflation by bots or repeated taps — mitigated by authentication, dedup window, and rate limits.
- **Implementation guidance**: Dedup window is configuration (`app.analytics.click-dedup-window`, default 30 min per owner/campaign). Raw clicks retention: 13 months, then only aggregates.
- **Migration/reversal**: Adding impressions is a new interaction type and columns; aggregation can move to a consumer of `CampaignClickRecorded.v1` if volume grows.
