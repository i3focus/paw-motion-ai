CREATE TABLE insights (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    pet_id UUID,
    text VARCHAR(1000) NOT NULL,
    provider VARCHAR(120) NOT NULL,
    model VARCHAR(120),
    fallback_used BOOLEAN NOT NULL,
    generated_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_insights_owner_generated_at ON insights (owner_id, generated_at DESC);
