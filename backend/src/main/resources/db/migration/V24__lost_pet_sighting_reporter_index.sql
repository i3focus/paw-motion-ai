-- C23: supports the per-(alert, reporter) sighting cap count. Additive and harmless after a code rollback.
CREATE INDEX IF NOT EXISTS idx_lost_pet_sightings_alert_reporter ON lost_pet_sightings (alert_id, reporter_owner_id);
