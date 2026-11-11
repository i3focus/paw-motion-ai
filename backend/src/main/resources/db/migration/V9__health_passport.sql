CREATE TABLE health_records (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    type VARCHAR(16) NOT NULL CHECK (type IN ('VACCINE', 'DEWORMING', 'VET_VISIT', 'MEDICATION')),
    name VARCHAR(120) NOT NULL,
    administered_on DATE,
    due_on DATE,
    vet_name VARCHAR(120),
    clinic_name VARCHAR(160),
    notes VARCHAR(500),
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CHECK (administered_on IS NOT NULL OR due_on IS NOT NULL)
);
CREATE INDEX idx_health_records_pet ON health_records (pet_id, due_on);

CREATE TABLE health_reminders (
    id UUID PRIMARY KEY,
    record_id UUID NOT NULL REFERENCES health_records(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    remind_at TIMESTAMPTZ NOT NULL,
    sent_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_health_reminders_record_time UNIQUE (record_id, remind_at)
);
CREATE INDEX idx_health_reminders_due ON health_reminders (remind_at) WHERE sent_at IS NULL;

CREATE TABLE pet_weights (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    measured_on DATE NOT NULL,
    weight_kg NUMERIC(5, 2) NOT NULL CHECK (weight_kg > 0),
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_pet_weights_day UNIQUE (pet_id, measured_on)
);

CREATE TABLE vet_share_links (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    token_hash VARCHAR(128) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);
