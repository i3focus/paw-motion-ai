-- PR 5a (C3, C21): plausible range for activities. Additive and non-destructive.
-- NOT VALID: enforced for new/updated rows only; legacy rows are neither checked nor changed.
-- The DB check is a backstop for the application policy (app.activity.max-backdate, default 14 days).
-- VALIDATE CONSTRAINT is intentionally left for a later migration, after reviewing the report below.
ALTER TABLE activities
    ADD CONSTRAINT chk_activities_duration_range
        CHECK (duration_seconds > 0 AND duration_seconds <= 86400) NOT VALID,
    ADD CONSTRAINT chk_activities_distance_range
        CHECK (distance_meters IS NULL OR (distance_meters >= 0 AND distance_meters <= 500000)) NOT VALID,
    ADD CONSTRAINT chk_activities_started_at_range
        CHECK (started_at >= created_at - interval '14 days' AND started_at <= created_at + interval '5 minutes') NOT VALID;

-- Report of legacy rows outside the range (log only; nothing is deleted or rewritten).
DO $$
DECLARE
    bad_duration BIGINT;
    bad_distance BIGINT;
    bad_started BIGINT;
BEGIN
    SELECT count(*) INTO bad_duration FROM activities WHERE NOT (duration_seconds > 0 AND duration_seconds <= 86400);
    SELECT count(*) INTO bad_distance FROM activities
        WHERE NOT (distance_meters IS NULL OR (distance_meters >= 0 AND distance_meters <= 500000));
    SELECT count(*) INTO bad_started FROM activities
        WHERE NOT (started_at >= created_at - interval '14 days' AND started_at <= created_at + interval '5 minutes');
    RAISE NOTICE 'activities outside plausible range (legacy, kept): duration=%, distance=%, started_at=%',
        bad_duration, bad_distance, bad_started;
END $$;
