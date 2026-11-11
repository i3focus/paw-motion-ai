CREATE TABLE communities (
    id UUID PRIMARY KEY,
    slug VARCHAR(80) NOT NULL UNIQUE,
    name VARCHAR(120) NOT NULL,
    kind VARCHAR(16) NOT NULL CHECK (kind IN ('BREED', 'NEIGHBORHOOD', 'CAUSE')),
    description VARCHAR(500),
    cover_url VARCHAR(500),
    location geography(Point, 4326),
    created_by UUID REFERENCES owners(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_communities_kind ON communities (kind);
CREATE INDEX idx_communities_name ON communities (lower(name));

CREATE TABLE community_members (
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    role VARCHAR(16) NOT NULL CHECK (role IN ('MEMBER', 'MODERATOR')),
    joined_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (community_id, owner_id)
);
CREATE INDEX idx_community_members_owner ON community_members (owner_id);

CREATE TABLE community_events (
    id UUID PRIMARY KEY,
    community_id UUID NOT NULL REFERENCES communities(id) ON DELETE CASCADE,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    location geography(Point, 4326),
    location_name VARCHAR(160),
    starts_at TIMESTAMPTZ NOT NULL,
    ends_at TIMESTAMPTZ,
    capacity INT CHECK (capacity > 0),
    created_by UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_community_events_start ON community_events (community_id, starts_at);

CREATE TABLE event_rsvps (
    event_id UUID NOT NULL REFERENCES community_events(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('GOING', 'INTERESTED')),
    created_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (event_id, owner_id)
);
