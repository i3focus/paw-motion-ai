-- PR 7b (N9, N2): supports the per-owner quota counts (total/active/daily). Additive and idempotent.
CREATE INDEX IF NOT EXISTS idx_communities_created_by ON communities (created_by, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_community_events_created_by ON community_events (created_by, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_activities_owner_created_at ON activities (owner_id, created_at DESC);
