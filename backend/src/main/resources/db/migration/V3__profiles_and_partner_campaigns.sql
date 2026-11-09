ALTER TABLE owners
    ADD COLUMN bio VARCHAR(500),
    ADD COLUMN phone VARCHAR(40),
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN business_name VARCHAR(160),
    ADD COLUMN website VARCHAR(300),
    ADD COLUMN partner_description VARCHAR(1000);

CREATE TABLE partner_campaigns (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    target_audience VARCHAR(160) NOT NULL,
    cta VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    ai_topics VARCHAR(2000),
    analyzed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_partner_campaigns_partner_id ON partner_campaigns (partner_id);
CREATE INDEX idx_partner_campaigns_status ON partner_campaigns (status);
