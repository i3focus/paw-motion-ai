-- C22: supports the 30-minute dedup lookup of ad events per (campaign, type, placement, viewer). Additive only.
CREATE INDEX IF NOT EXISTS idx_campaign_events_dedup
    ON campaign_events (campaign_id, type, placement, viewer_hash, occurred_at DESC);
