-- C9: record which invites produced a notification, to deduplicate per organizer/owner window and cap per day.
-- Additive. Existing rows count as notified. Unused after a code rollback.
ALTER TABLE playdate_invites ADD COLUMN IF NOT EXISTS notified BOOLEAN NOT NULL DEFAULT TRUE;
CREATE INDEX IF NOT EXISTS idx_playdates_organizer_created ON playdates (organizer_owner_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_playdate_invites_owner_created ON playdate_invites (invitee_owner_id, created_at DESC) WHERE notified;
