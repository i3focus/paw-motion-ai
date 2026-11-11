CREATE TABLE places (
    id UUID PRIMARY KEY,
    name VARCHAR(160) NOT NULL,
    category VARCHAR(16) NOT NULL CHECK (category IN ('PARK', 'VET', 'CAFE', 'HOTEL', 'GROOMING', 'PET_SHOP', 'TRAINING', 'OTHER')),
    location geography(Point, 4326) NOT NULL,
    address VARCHAR(300),
    description VARCHAR(500),
    rating NUMERIC(2, 1) CHECK (rating BETWEEN 0 AND 5),
    rating_count INT NOT NULL DEFAULT 0,
    partner_id UUID REFERENCES owners(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_places_location ON places USING GIST (location);
CREATE INDEX idx_places_category ON places (category);
CREATE UNIQUE INDEX uq_places_partner ON places (partner_id) WHERE partner_id IS NOT NULL;

INSERT INTO places (id, name, category, location, description, partner_id, created_at, updated_at)
SELECT gen_random_uuid(), COALESCE(NULLIF(business_name, ''), display_name), 'OTHER', location,
       LEFT(partner_description, 500), id, created_at, now()
FROM owners
WHERE role = 'PARTNER' AND location IS NOT NULL;

CREATE TABLE check_ins (
    id UUID PRIMARY KEY,
    place_id UUID NOT NULL REFERENCES places(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_check_ins_place_time ON check_ins (place_id, created_at DESC);
CREATE INDEX idx_check_ins_pet ON check_ins (pet_id);

CREATE TABLE playdates (
    id UUID PRIMARY KEY,
    organizer_owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    organizer_pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    place_id UUID REFERENCES places(id) ON DELETE SET NULL,
    location geography(Point, 4326),
    location_name VARCHAR(160),
    mode VARCHAR(16) NOT NULL CHECK (mode IN ('NOW', 'SCHEDULED')),
    starts_at TIMESTAMPTZ NOT NULL,
    message VARCHAR(300),
    status VARCHAR(16) NOT NULL CHECK (status IN ('OPEN', 'CANCELLED', 'DONE')),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_playdates_location ON playdates USING GIST (location);

CREATE TABLE playdate_invites (
    id UUID PRIMARY KEY,
    playdate_id UUID NOT NULL REFERENCES playdates(id) ON DELETE CASCADE,
    invitee_pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    invitee_owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    status VARCHAR(16) NOT NULL CHECK (status IN ('PENDING', 'ACCEPTED', 'DECLINED')),
    responded_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_playdate_invites UNIQUE (playdate_id, invitee_pet_id)
);
CREATE INDEX idx_playdate_invites_owner ON playdate_invites (invitee_owner_id, status);
