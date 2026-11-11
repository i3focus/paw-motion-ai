CREATE TABLE xp_events (
    id UUID PRIMARY KEY,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    source_type VARCHAR(32) NOT NULL,
    source_key VARCHAR(160) NOT NULL,
    points INT NOT NULL CHECK (points > 0),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_xp_events_source UNIQUE (pet_id, source_key)
);
CREATE INDEX idx_xp_events_owner_time ON xp_events (owner_id, occurred_at DESC);

INSERT INTO xp_events (id, pet_id, owner_id, source_type, source_key, points, occurred_at)
SELECT gen_random_uuid(), a.pet_id, a.owner_id, 'ACTIVITY', 'activity:' || a.id,
       10 + LEAST(50, FLOOR(COALESCE(a.distance_meters, 0) / 1000)::INT * 5), a.started_at
FROM activities a
JOIN owners o ON o.id = a.owner_id
JOIN pets p ON p.id = a.pet_id;

CREATE TABLE badges (
    code VARCHAR(48) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(200) NOT NULL,
    emoji VARCHAR(16) NOT NULL,
    criteria VARCHAR(32) NOT NULL CHECK (criteria IN ('ACTIVITY_COUNT', 'DISTANCE_METERS', 'STREAK_DAYS', 'POST_COUNT', 'CHECK_IN_COUNT', 'PLAYDATE_COUNT')),
    threshold BIGINT NOT NULL CHECK (threshold > 0)
);

INSERT INTO badges (code, name, description, emoji, criteria, threshold) VALUES
    ('FIRST_WALK', 'First steps', 'Logged the first adventure', '🐾', 'ACTIVITY_COUNT', 1),
    ('TEN_ADVENTURES', 'Adventurer', 'Logged 10 adventures', '🧭', 'ACTIVITY_COUNT', 10),
    ('MARATHON', 'Marathon paws', 'Walked 42 km in total', '🏅', 'DISTANCE_METERS', 42195),
    ('CENTURY', 'Century club', 'Walked 100 km in total', '🏆', 'DISTANCE_METERS', 100000),
    ('STREAK_7', 'On fire', 'Walked 7 days in a row', '🔥', 'STREAK_DAYS', 7),
    ('STREAK_30', 'Unstoppable', 'Walked 30 days in a row', '⚡', 'STREAK_DAYS', 30),
    ('STORYTELLER', 'Storyteller', 'Shared 10 posts', '📸', 'POST_COUNT', 10),
    ('PARK_EXPLORER', 'Park explorer', 'Checked in at 5 places', '🌳', 'CHECK_IN_COUNT', 5),
    ('PLAYDATE_PAL', 'Playdate pal', 'Joined 3 playdates', '🎾', 'PLAYDATE_COUNT', 3);

CREATE TABLE pet_badges (
    id UUID NOT NULL DEFAULT gen_random_uuid() UNIQUE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    badge_code VARCHAR(48) NOT NULL REFERENCES badges(code),
    awarded_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (pet_id, badge_code)
);

INSERT INTO pet_badges (pet_id, badge_code, awarded_at)
SELECT s.pet_id, b.code, s.last_at
FROM (
    SELECT a.pet_id, COUNT(*) AS activity_count, COALESCE(SUM(a.distance_meters), 0) AS distance, MAX(a.started_at) AS last_at
    FROM activities a JOIN pets p ON p.id = a.pet_id
    GROUP BY a.pet_id
) s
JOIN badges b ON (b.criteria = 'ACTIVITY_COUNT' AND s.activity_count >= b.threshold)
              OR (b.criteria = 'DISTANCE_METERS' AND s.distance >= b.threshold);

CREATE TABLE challenges (
    id UUID PRIMARY KEY,
    title VARCHAR(120) NOT NULL,
    description VARCHAR(300) NOT NULL,
    metric VARCHAR(32) NOT NULL CHECK (metric IN ('ACTIVITY_COUNT', 'DISTANCE_METERS', 'DURATION_SECONDS')),
    target_value BIGINT NOT NULL CHECK (target_value > 0),
    xp_reward INT NOT NULL CHECK (xp_reward > 0),
    starts_on DATE NOT NULL,
    ends_on DATE NOT NULL,
    author VARCHAR(16) NOT NULL CHECK (author IN ('ADMIN', 'AI', 'SYSTEM')),
    created_by UUID REFERENCES owners(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CHECK (ends_on >= starts_on)
);
CREATE INDEX idx_challenges_window ON challenges (starts_on, ends_on);

CREATE TABLE challenge_participants (
    challenge_id UUID NOT NULL REFERENCES challenges(id) ON DELETE CASCADE,
    pet_id UUID NOT NULL REFERENCES pets(id) ON DELETE CASCADE,
    owner_id UUID NOT NULL REFERENCES owners(id) ON DELETE CASCADE,
    joined_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    PRIMARY KEY (challenge_id, pet_id)
);
