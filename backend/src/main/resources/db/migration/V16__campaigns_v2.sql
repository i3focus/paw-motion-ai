ALTER TABLE partner_campaigns
    ADD COLUMN media_asset_id UUID REFERENCES media_assets(id) ON DELETE SET NULL,
    ADD COLUMN cta_type VARCHAR(16) NOT NULL DEFAULT 'LINK' CHECK (cta_type IN ('LINK', 'BOOKING')),
    ADD COLUMN cta_url VARCHAR(500),
    ADD COLUMN service_id UUID REFERENCES partner_services(id) ON DELETE SET NULL,
    ADD COLUMN budget_credits BIGINT NOT NULL DEFAULT 0 CHECK (budget_credits >= 0),
    ADD COLUMN starts_at TIMESTAMPTZ,
    ADD COLUMN ends_at TIMESTAMPTZ,
    ADD COLUMN target_location geography(Point, 4326),
    ADD COLUMN target_radius_m INT CHECK (target_radius_m BETWEEN 500 AND 100000),
    ADD COLUMN target_species VARCHAR(32)[] NOT NULL DEFAULT '{}',
    ADD COLUMN target_breeds VARCHAR(80)[] NOT NULL DEFAULT '{}',
    ADD COLUMN target_interests VARCHAR(48)[] NOT NULL DEFAULT '{}',
    ADD COLUMN formats VARCHAR(24)[] NOT NULL DEFAULT '{FEED_CARD}',
    ADD COLUMN reviewed_by UUID REFERENCES owners(id) ON DELETE SET NULL,
    ADD COLUMN reviewed_at TIMESTAMPTZ,
    ADD COLUMN rejection_reason VARCHAR(500),
    ADD COLUMN end_reason VARCHAR(24) CHECK (end_reason IN ('ARCHIVED', 'EXPIRED', 'ENDED_BY_PARTNER', 'BUDGET_EXHAUSTED'));

UPDATE partner_campaigns c
SET budget_credits = c.credits_charged,
    starts_at = c.created_at,
    ends_at = c.expires_at,
    cta_url = o.website,
    target_location = o.location,
    target_radius_m = CASE WHEN o.location IS NULL THEN NULL ELSE 10000 END,
    reviewed_at = CASE WHEN c.status = 'ACTIVE' THEN c.updated_at END
FROM owners o
WHERE o.id = c.partner_id;

UPDATE partner_campaigns SET starts_at = created_at, ends_at = expires_at WHERE starts_at IS NULL;

UPDATE partner_campaigns SET end_reason = status, status = 'ENDED' WHERE status IN ('ARCHIVED', 'EXPIRED');

ALTER TABLE partner_campaigns
    ALTER COLUMN starts_at SET NOT NULL,
    ALTER COLUMN ends_at SET NOT NULL,
    ADD CONSTRAINT ck_partner_campaigns_status CHECK (status IN ('DRAFT', 'IN_REVIEW', 'ACTIVE', 'PAUSED', 'ENDED')),
    ADD CONSTRAINT ck_partner_campaigns_window CHECK (ends_at > starts_at),
    ADD CONSTRAINT ck_partner_campaigns_formats CHECK (formats <@ ARRAY['FEED_CARD', 'MAP_PIN', 'DISCOVER_HIGHLIGHT', 'NEARBY_PROMO', 'AI_RECOMMENDATION']::VARCHAR(24)[]);

CREATE INDEX idx_partner_campaigns_serving ON partner_campaigns (status, starts_at, ends_at);
CREATE INDEX idx_partner_campaigns_target_location ON partner_campaigns USING GIST (target_location);

CREATE TABLE campaign_events (
    id UUID PRIMARY KEY,
    campaign_id UUID NOT NULL REFERENCES partner_campaigns(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL CHECK (type IN ('IMPRESSION', 'CLICK', 'BOOKING')),
    placement VARCHAR(24) NOT NULL CHECK (placement IN ('FEED_CARD', 'MAP_PIN', 'DISCOVER_HIGHLIGHT', 'NEARBY_PROMO', 'AI_RECOMMENDATION')),
    viewer_hash VARCHAR(64) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_campaign_events_campaign_time ON campaign_events (campaign_id, occurred_at);
CREATE INDEX idx_campaign_events_viewer_time ON campaign_events (viewer_hash, occurred_at DESC);
