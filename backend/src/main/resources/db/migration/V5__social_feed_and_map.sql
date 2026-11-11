ALTER TABLE owners
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION;

ALTER TABLE activities
    ADD COLUMN latitude DOUBLE PRECISION,
    ADD COLUMN longitude DOUBLE PRECISION,
    ADD COLUMN share_to_paw_city BOOLEAN NOT NULL DEFAULT FALSE;

CREATE TABLE activity_reactions (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL CHECK (type IN ('LIKE', 'LOVE', 'CHEER')),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_activity_reactions_activity_owner UNIQUE (activity_id, owner_id)
);

CREATE INDEX idx_activity_reactions_activity_id ON activity_reactions (activity_id);
CREATE INDEX idx_activity_reactions_owner_id ON activity_reactions (owner_id);

CREATE TABLE activity_comments (
    id UUID PRIMARY KEY,
    activity_id UUID NOT NULL REFERENCES activities(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    body VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_activity_comments_activity_created ON activity_comments (activity_id, created_at);
CREATE INDEX idx_activity_comments_owner_id ON activity_comments (owner_id);
