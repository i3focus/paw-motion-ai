CREATE TABLE push_tokens (
    token VARCHAR(200) PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    platform VARCHAR(8) NOT NULL CHECK (platform IN ('IOS', 'ANDROID', 'WEB')),
    created_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_push_tokens_owner ON push_tokens (owner_id);

CREATE TABLE notification_preferences (
    owner_id UUID PRIMARY KEY REFERENCES owners(id) ON DELETE CASCADE,
    health_reminders BOOLEAN NOT NULL DEFAULT TRUE,
    social BOOLEAN NOT NULL DEFAULT TRUE,
    playdates BOOLEAN NOT NULL DEFAULT TRUE,
    lost_pets BOOLEAN NOT NULL DEFAULT TRUE,
    bookings BOOLEAN NOT NULL DEFAULT TRUE,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE notifications (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    type VARCHAR(32) NOT NULL,
    title VARCHAR(120) NOT NULL,
    body VARCHAR(300) NOT NULL,
    data JSONB NOT NULL DEFAULT '{}'::jsonb,
    read_at TIMESTAMPTZ,
    sent_at TIMESTAMPTZ,
    error VARCHAR(300),
    created_at TIMESTAMPTZ NOT NULL
);
CREATE INDEX idx_notifications_owner_created ON notifications (owner_id, created_at DESC);
