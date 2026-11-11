-- Phase 6 cleanup: drop legacy social tables (data already lives in reactions/comments)
-- and owner business columns (data already lives in partner_profiles).
DROP TABLE IF EXISTS activity_comments;
DROP TABLE IF EXISTS activity_reactions;

ALTER TABLE owners
    DROP COLUMN IF EXISTS business_name,
    DROP COLUMN IF EXISTS website,
    DROP COLUMN IF EXISTS partner_description,
    DROP COLUMN IF EXISTS latitude,
    DROP COLUMN IF EXISTS longitude;
