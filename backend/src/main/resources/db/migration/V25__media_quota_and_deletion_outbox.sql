-- C10: per-owner media quota (atomic reservation) and post-commit file deletion outbox.
-- Additive: no existing row is changed or removed. Rollback: set APP_MEDIA_MODE=off and APP_MEDIA_DELETION_ENABLED=false; the tables stay.
CREATE TABLE owner_media_usage (
    owner_id UUID PRIMARY KEY REFERENCES owners(id) ON DELETE CASCADE,
    bytes BIGINT NOT NULL DEFAULT 0 CHECK (bytes >= 0),
    objects BIGINT NOT NULL DEFAULT 0 CHECK (objects >= 0)
);

INSERT INTO owner_media_usage (owner_id, bytes, objects)
SELECT owner_id, SUM(size_bytes), COUNT(*) FROM media_assets GROUP BY owner_id
ON CONFLICT (owner_id) DO NOTHING;

CREATE TABLE media_deletions (
    id BIGSERIAL PRIMARY KEY,
    storage_key VARCHAR(300) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT now(),
    processed_at TIMESTAMPTZ,
    attempts INT NOT NULL DEFAULT 0,
    last_error VARCHAR(300)
);
CREATE INDEX idx_media_deletions_pending ON media_deletions (id) WHERE processed_at IS NULL;

-- Any deletion of a media_assets row (explicit or by FK cascade) releases the quota and enqueues the file deletion in the same transaction.
CREATE FUNCTION media_assets_after_delete() RETURNS trigger AS $$
BEGIN
    UPDATE owner_media_usage
       SET bytes = GREATEST(bytes - OLD.size_bytes, 0), objects = GREATEST(objects - 1, 0)
     WHERE owner_id = OLD.owner_id;
    INSERT INTO media_deletions (storage_key) VALUES (OLD.storage_key);
    RETURN OLD;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_media_assets_after_delete AFTER DELETE ON media_assets
    FOR EACH ROW EXECUTE FUNCTION media_assets_after_delete();
