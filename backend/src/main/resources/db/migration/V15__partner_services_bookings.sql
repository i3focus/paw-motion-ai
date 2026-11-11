CREATE TABLE partner_profiles (
    partner_id UUID PRIMARY KEY REFERENCES owners(id) ON DELETE CASCADE,
    business_name VARCHAR(160) NOT NULL,
    category VARCHAR(16) NOT NULL DEFAULT 'OTHER' CHECK (category IN ('VET', 'GROOMING', 'HOTEL', 'CAFE', 'PET_SHOP', 'TRAINING', 'WALKER', 'OTHER')),
    description VARCHAR(1000),
    website VARCHAR(300),
    phone VARCHAR(40),
    address VARCHAR(300),
    location geography(Point, 4326),
    opening_hours VARCHAR(300),
    cover_url VARCHAR(500),
    verified BOOLEAN NOT NULL DEFAULT FALSE,
    verified_at TIMESTAMPTZ,
    verified_by UUID REFERENCES owners(id) ON DELETE SET NULL,
    place_id UUID REFERENCES places(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_partner_profiles_location ON partner_profiles USING GIST (location);

INSERT INTO partner_profiles (partner_id, business_name, description, website, phone, location, place_id, created_at, updated_at)
SELECT o.id, COALESCE(NULLIF(o.business_name, ''), o.display_name), o.partner_description, o.website, o.phone, o.location,
       pl.id, o.created_at, now()
FROM owners o
LEFT JOIN places pl ON pl.partner_id = o.id
WHERE o.role = 'PARTNER';

CREATE TABLE partner_services (
    id UUID PRIMARY KEY,
    partner_id UUID NOT NULL REFERENCES partner_profiles(partner_id) ON DELETE CASCADE,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(500),
    duration_minutes INT NOT NULL CHECK (duration_minutes BETWEEN 5 AND 1440),
    price_cents BIGINT NOT NULL CHECK (price_cents >= 0),
    currency CHAR(3) NOT NULL DEFAULT 'BRL',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_partner_services_partner ON partner_services (partner_id) WHERE active;

CREATE TABLE bookings (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    partner_id UUID NOT NULL REFERENCES partner_profiles(partner_id) ON DELETE CASCADE,
    service_id UUID NOT NULL REFERENCES partner_services(id),
    starts_at TIMESTAMPTZ NOT NULL,
    status VARCHAR(16) NOT NULL CHECK (status IN ('REQUESTED', 'CONFIRMED', 'CANCELLED', 'COMPLETED')),
    source VARCHAR(8) NOT NULL CHECK (source IN ('APP', 'AI', 'AD')),
    campaign_id UUID REFERENCES partner_campaigns(id) ON DELETE SET NULL,
    ai_action_id UUID REFERENCES ai_actions(id) ON DELETE SET NULL,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);
CREATE INDEX idx_bookings_partner_start ON bookings (partner_id, starts_at);
CREATE INDEX idx_bookings_owner ON bookings (owner_id, starts_at DESC);
