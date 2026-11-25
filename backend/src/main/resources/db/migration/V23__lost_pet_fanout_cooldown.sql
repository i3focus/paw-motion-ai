-- C8/C18: records when an alert triggered the nearby fan-out, to enforce the per-pet cooldown and per-owner daily cap.
-- Additive. Existing rows keep NULL (no recorded fan-out). Unused after a code rollback.
ALTER TABLE lost_pet_alerts ADD COLUMN IF NOT EXISTS fanout_sent_at TIMESTAMPTZ;
CREATE INDEX IF NOT EXISTS idx_lost_pet_alerts_pet_fanout ON lost_pet_alerts (pet_id, fanout_sent_at DESC) WHERE fanout_sent_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_lost_pet_alerts_owner_created ON lost_pet_alerts (owner_id, created_at DESC);
