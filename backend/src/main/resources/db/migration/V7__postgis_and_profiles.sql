CREATE EXTENSION IF NOT EXISTS postgis;

ALTER TABLE owners
    ADD COLUMN location geography(Point, 4326),
    ADD COLUMN plan VARCHAR(16) NOT NULL DEFAULT 'FREE' CHECK (plan IN ('FREE', 'PREMIUM')),
    ADD COLUMN neighborhood VARCHAR(120),
    ADD COLUMN city VARCHAR(120);

UPDATE owners
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE INDEX idx_owners_location ON owners USING GIST (location);
CREATE INDEX idx_owners_neighborhood ON owners (lower(neighborhood));

ALTER TABLE pets
    ADD COLUMN avatar_url VARCHAR(500),
    ADD COLUMN cover_url VARCHAR(500),
    ADD COLUMN bio VARCHAR(300);

ALTER TABLE activities
    ADD COLUMN location geography(Point, 4326),
    ADD COLUMN route geography(LineString, 4326);

UPDATE activities
SET location = ST_SetSRID(ST_MakePoint(longitude, latitude), 4326)::geography
WHERE latitude IS NOT NULL AND longitude IS NOT NULL;

CREATE INDEX idx_activities_location ON activities USING GIST (location);
