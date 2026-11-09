CREATE TABLE owners (
    id UUID PRIMARY KEY,
    email VARCHAR(320) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_owners_email UNIQUE (email)
);

CREATE TABLE pets (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    name VARCHAR(80) NOT NULL,
    species VARCHAR(32) NOT NULL,
    breed VARCHAR(80),
    birth_date DATE,
    size VARCHAR(32) NOT NULL,
    activity_preference VARCHAR(120),
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_pets_owner_id ON pets (owner_id);
CREATE INDEX idx_pets_owner_status ON pets (owner_id, status);

CREATE TABLE activities (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL,
    pet_id UUID NOT NULL,
    type VARCHAR(32) NOT NULL,
    started_at TIMESTAMPTZ NOT NULL,
    duration_seconds BIGINT NOT NULL,
    distance_meters DOUBLE PRECISION,
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_activities_owner_started_at ON activities (owner_id, started_at DESC);
CREATE INDEX idx_activities_pet_started_at ON activities (pet_id, started_at DESC);
