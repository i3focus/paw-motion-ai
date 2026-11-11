CREATE TABLE lost_pet_alerts (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    photo_media_id UUID REFERENCES media_assets(id) ON DELETE SET NULL,
    description VARCHAR(500) NOT NULL,
    contact_phone VARCHAR(40),
    last_seen_location geography(Point, 4326) NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    radius_m INT NOT NULL CHECK (radius_m BETWEEN 100 AND 20000),
    status VARCHAR(16) NOT NULL CHECK (status IN ('ACTIVE', 'FOUND', 'CANCELLED')),
    created_at TIMESTAMPTZ NOT NULL,
    resolved_at TIMESTAMPTZ
);
CREATE INDEX idx_lost_pet_alerts_location ON lost_pet_alerts USING GIST (last_seen_location) WHERE status = 'ACTIVE';
CREATE UNIQUE INDEX uq_lost_pet_alerts_active_pet ON lost_pet_alerts (pet_id) WHERE status = 'ACTIVE';

CREATE TABLE lost_pet_sightings (
    id UUID PRIMARY KEY,
    alert_id UUID NOT NULL REFERENCES lost_pet_alerts(id) ON DELETE CASCADE,
    reporter_owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    location geography(Point, 4326) NOT NULL,
    seen_at TIMESTAMPTZ NOT NULL,
    note VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_lost_pet_sightings_alert ON lost_pet_sightings (alert_id, seen_at DESC);
