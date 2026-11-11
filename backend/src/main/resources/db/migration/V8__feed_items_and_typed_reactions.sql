CREATE TABLE media_assets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    storage_key VARCHAR(300) NOT NULL UNIQUE,
    content_type VARCHAR(80) NOT NULL,
    size_bytes BIGINT NOT NULL CHECK (size_bytes > 0),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_media_assets_owner ON media_assets (owner_id);

CREATE TABLE posts (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    caption VARCHAR(200),
    place_name VARCHAR(160),
    location geography(Point, 4326),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_posts_pet_created ON posts (pet_id, created_at DESC);

CREATE TABLE post_media (
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    position SMALLINT NOT NULL,
    PRIMARY KEY (post_id, media_id)
);

CREATE TABLE post_pet_tags (
    post_id UUID NOT NULL REFERENCES posts(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    PRIMARY KEY (post_id, pet_id)
);

CREATE TABLE stories (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    media_id UUID NOT NULL REFERENCES media_assets(id) ON DELETE CASCADE,
    caption VARCHAR(120),
    created_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_stories_pet_created ON stories (pet_id, created_at DESC);
CREATE INDEX idx_stories_expires ON stories (expires_at);

CREATE TABLE story_views (
    story_id UUID NOT NULL REFERENCES stories(id) ON DELETE CASCADE,
    viewer_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    viewed_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (story_id, viewer_id)
);

CREATE TABLE feed_items (
    id UUID PRIMARY KEY,
    type VARCHAR(16) NOT NULL CHECK (type IN ('POST', 'ACTIVITY', 'ACHIEVEMENT')),
    ref_id UUID NOT NULL,
    author_owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    author_pet_id UUID REFERENCES pets(id) ON DELETE CASCADE,
    location geography(Point, 4326),
    published_at TIMESTAMPTZ NOT NULL,
    deleted_at TIMESTAMPTZ,
    CONSTRAINT uq_feed_items_type_ref UNIQUE (type, ref_id)
);
CREATE INDEX idx_feed_items_published ON feed_items (published_at DESC) WHERE deleted_at IS NULL;
CREATE INDEX idx_feed_items_pet_published ON feed_items (author_pet_id, published_at DESC);
CREATE INDEX idx_feed_items_location ON feed_items USING GIST (location);

INSERT INTO feed_items (id, type, ref_id, author_owner_id, author_pet_id, location, published_at)
SELECT gen_random_uuid(), 'ACTIVITY', a.id, a.owner_id, a.pet_id, a.location, a.started_at
FROM activities a
JOIN owners o ON o.id = a.owner_id
JOIN pets p ON p.id = a.pet_id
WHERE a.share_to_paw_city = TRUE;

CREATE TABLE reactions (
    id UUID PRIMARY KEY,
    feed_item_id UUID NOT NULL REFERENCES feed_items(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL CHECK (type IN ('PAW', 'LAUGH', 'HEART')),
    legacy_type VARCHAR(16),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_reactions_item_owner UNIQUE (feed_item_id, owner_id)
);
CREATE INDEX idx_reactions_owner ON reactions (owner_id);

INSERT INTO reactions (id, feed_item_id, owner_id, type, legacy_type, created_at, updated_at)
SELECT r.id, f.id, r.owner_id,
       CASE r.type WHEN 'LIKE' THEN 'PAW' WHEN 'LOVE' THEN 'HEART' ELSE 'LAUGH' END,
       r.type, r.created_at, r.updated_at
FROM activity_reactions r
JOIN feed_items f ON f.type = 'ACTIVITY' AND f.ref_id = r.activity_id;

CREATE TABLE comments (
    id UUID PRIMARY KEY,
    feed_item_id UUID NOT NULL REFERENCES feed_items(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    body VARCHAR(500) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_comments_item_created ON comments (feed_item_id, created_at);

INSERT INTO comments (id, feed_item_id, owner_id, body, created_at)
SELECT c.id, f.id, c.owner_id, c.body, c.created_at
FROM activity_comments c
JOIN feed_items f ON f.type = 'ACTIVITY' AND f.ref_id = c.activity_id;

CREATE TABLE pet_follows (
    follower_owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (follower_owner_id, pet_id)
);
CREATE INDEX idx_pet_follows_pet ON pet_follows (pet_id);
