-- C13: one insight per (owner, pet, day) for NEW rows. Additive: legacy rows keep bucket_date NULL and are never touched.
ALTER TABLE insights ADD COLUMN bucket_date DATE;

CREATE UNIQUE INDEX uq_insights_owner_pet_bucket
    ON insights (owner_id, (COALESCE(pet_id, '00000000-0000-0000-0000-000000000000'::uuid)), bucket_date)
    WHERE bucket_date IS NOT NULL;
