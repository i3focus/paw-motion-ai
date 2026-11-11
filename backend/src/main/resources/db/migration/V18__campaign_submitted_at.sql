ALTER TABLE partner_campaigns ADD COLUMN IF NOT EXISTS submitted_at TIMESTAMPTZ;

UPDATE partner_campaigns SET submitted_at = updated_at WHERE submitted_at IS NULL AND status <> 'DRAFT';
