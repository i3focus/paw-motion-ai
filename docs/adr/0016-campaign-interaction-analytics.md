# ADR-0016: Campaign Interaction Tracking and Aggregate Partner Analytics

- **Status**: Proposed
- **Context**: Partners need to see clicks and reach of their campaigns to perceive value and keep buying credits. Partner analytics must stay aggregate-only (REQ-NFR-001, LGPD).
- **Decision**: Owner clients post `IMPRESSION`/`CLICK` interactions to `POST /api/campaigns/{campaignId}/interactions`. The Partner context stores raw interactions in `campaign_interactions` (owner id kept only as a salted hash for unique counting) and maintains a daily aggregate table `campaign_daily_metrics` updated in the same transaction (upsert). Partner dashboard endpoints read only aggregates and ledger debits; breakdowns below a k-anonymity threshold are grouped.
- **Alternatives**: Third-party analytics SDK (rejected: data leaves platform, harder to keep aggregate-only); computing aggregates on each request from raw data (acceptable for tiny volumes, rejected for predictable latency); separate analytics database (premature).
- **Rationale**: Simple, in-database, privacy-preserving, and sufficient for MVP volumes.
- **Positive consequences**: Real-time-ish metrics (clicks, CTR, cost per click) linked to credits spent.
- **Negative consequences**: Write load from impressions; mitigated by client batching and rate limiting.
- **Risks**: Click inflation by bots or repeated taps — mitigated by authentication, dedup window, and rate limits.
- **Implementation guidance**: Dedup window and k threshold are configuration (`app.analytics.click-dedup-window`, `app.analytics.k-anonymity-min`). Raw interactions retention: 13 months, then only aggregates.
- **Migration/reversal**: Move aggregation to an event consumer of `CampaignInteractionRecorded.v1` if volume grows.
