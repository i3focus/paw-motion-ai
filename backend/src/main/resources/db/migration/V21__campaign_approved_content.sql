-- C4: content approved by moderation. Additive; columns stay unused after a code rollback.
ALTER TABLE partner_campaigns
    ADD COLUMN IF NOT EXISTS approved_content_hash VARCHAR(64),
    ADD COLUMN IF NOT EXISTS approved_ends_at TIMESTAMPTZ;

-- Canonical SHA-256 of the moderated content (title, description, cta, cta_type, cta_url, service_id,
-- media_asset_id, formats, target_*). Arrays are sorted so the hash is stable under reordering.
-- ends_at is tracked separately in approved_ends_at.
CREATE OR REPLACE FUNCTION campaign_content_hash(p_campaign_id UUID) RETURNS VARCHAR AS $$
    SELECT encode(sha256(convert_to(jsonb_build_object(
        'title', c.title,
        'description', c.description,
        'cta', c.cta,
        'cta_type', c.cta_type,
        'cta_url', c.cta_url,
        'service_id', c.service_id,
        'media_asset_id', c.media_asset_id,
        'formats', (SELECT COALESCE(jsonb_agg(x ORDER BY x), '[]'::jsonb) FROM unnest(c.formats) AS x),
        'target_audience', c.target_audience,
        'target_species', (SELECT COALESCE(jsonb_agg(x ORDER BY x), '[]'::jsonb) FROM unnest(c.target_species) AS x),
        'target_breeds', (SELECT COALESCE(jsonb_agg(x ORDER BY x), '[]'::jsonb) FROM unnest(c.target_breeds) AS x),
        'target_interests', (SELECT COALESCE(jsonb_agg(x ORDER BY x), '[]'::jsonb) FROM unnest(c.target_interests) AS x),
        'target_location', ST_AsText(c.target_location::geometry),
        'target_radius_m', c.target_radius_m
    )::text, 'UTF8')), 'hex')
    FROM partner_campaigns c WHERE c.id = p_campaign_id
$$ LANGUAGE sql STABLE;

-- Backfill: ACTIVE campaigns are already approved. Legacy PAUSED keep a NULL hash and need re-approval on resume.
UPDATE partner_campaigns
SET approved_content_hash = campaign_content_hash(id), approved_ends_at = ends_at
WHERE status = 'ACTIVE' AND approved_content_hash IS NULL;
