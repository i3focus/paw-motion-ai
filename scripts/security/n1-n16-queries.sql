\set ON_ERROR_STOP off
\timing off
SELECT id AS viewer FROM owners ORDER BY id LIMIT 1 \gset
SELECT owner_id AS heavy_owner, id AS heavy_pet FROM pets ORDER BY id LIMIT 1 \gset
SELECT id AS item FROM feed_items OFFSET 5 LIMIT 1 \gset
\echo '=== LEADERBOARD global (N1) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id, p.name, p.avatar_url, o.display_name, o.neighborhood, SUM(x.points) AS weekly_xp, (p.owner_id = :'viewer') AS own
FROM xp_events x JOIN pets p ON p.id = x.pet_id AND p.status = 'ACTIVE' JOIN owners o ON o.id = p.owner_id AND o.status = 'ACTIVE'
WHERE x.occurred_at > now() - interval '7 days'
GROUP BY p.id, p.name, p.avatar_url, o.display_name, o.neighborhood, p.owner_id ORDER BY weekly_xp DESC, p.id LIMIT 50;
\echo '=== LEADERBOARD neighborhood (N1) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT p.id, SUM(x.points) AS weekly_xp
FROM xp_events x JOIN pets p ON p.id = x.pet_id AND p.status = 'ACTIVE' JOIN owners o ON o.id = p.owner_id AND o.status = 'ACTIVE'
WHERE x.occurred_at > now() - interval '7 days' AND lower(o.neighborhood) = lower('Bairro 7')
GROUP BY p.id, p.owner_id ORDER BY weekly_xp DESC, p.id LIMIT 50;
\echo '=== DISCOVER communities (N1) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.*, COALESCE(mc.members, 0) AS members,
       (SELECT COUNT(*) FROM community_events e WHERE e.community_id = c.id AND e.starts_at > now()) AS upcoming, m.role AS my_role
FROM communities c
LEFT JOIN (SELECT community_id, COUNT(*) AS members FROM community_members GROUP BY community_id) mc ON mc.community_id = c.id
LEFT JOIN community_members m ON m.community_id = c.id AND m.owner_id = :'viewer'
WHERE (NULL::text IS NULL OR c.kind = 'BREED') ORDER BY members DESC, c.name LIMIT 21 OFFSET 0;
\echo '=== DISCOVER communities busca ILIKE (N1) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.*, COALESCE(mc.members, 0) AS members
FROM communities c LEFT JOIN (SELECT community_id, COUNT(*) AS members FROM community_members GROUP BY community_id) mc ON mc.community_id = c.id WHERE c.name ILIKE '%99%' OR c.description ILIKE '%99%' ORDER BY members DESC, c.name LIMIT 21 OFFSET 0;
\echo '=== FEED candidates (N11) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT f.id, f.type, f.ref_id, f.author_owner_id, f.author_pet_id, f.published_at, o.display_name, o.neighborhood, p.name,
  (f.author_owner_id = :'viewer') AS own,
  EXISTS (SELECT 1 FROM pet_follows pf WHERE pf.follower_owner_id = :'viewer' AND pf.pet_id = f.author_pet_id) AS followed,
  (SELECT COUNT(*) FROM reactions r WHERE r.feed_item_id = f.id) AS rc, (SELECT COUNT(*) FROM comments c WHERE c.feed_item_id = f.id) AS cc
FROM feed_items f JOIN owners o ON o.id = f.author_owner_id AND o.status = 'ACTIVE' LEFT JOIN pets p ON p.id = f.author_pet_id
WHERE f.deleted_at IS NULL AND f.published_at >= now() - interval '14 days'
 AND (f.author_owner_id = :'viewer'
   OR EXISTS (SELECT 1 FROM pet_follows pf WHERE pf.follower_owner_id = :'viewer' AND pf.pet_id = f.author_pet_id)
   OR (true AND f.location IS NOT NULL AND ST_DWithin(f.location, ST_SetSRID(ST_MakePoint(-46.33, -23.25), 4326)::geography, 10000))
   OR (lower(o.neighborhood) = lower('Bairro 7')))
ORDER BY f.published_at DESC LIMIT 500;
\echo '=== PLACES nearby 5km ILIKE (N11) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT pl.id, ST_Distance(pl.location, ST_SetSRID(ST_MakePoint(-46.33, -23.25), 4326)::geography) AS distance,
  (SELECT COUNT(*) FROM check_ins c WHERE c.place_id = pl.id AND c.created_at > now() - interval '1 day') AS ci
FROM places pl LEFT JOIN partner_profiles pp ON pp.partner_id = pl.partner_id
WHERE ST_DWithin(pl.location, ST_SetSRID(ST_MakePoint(-46.33, -23.25), 4326)::geography, 5000)
  AND pl.name ILIKE '%park%' ORDER BY distance ASC, pl.id LIMIT 21 OFFSET 0;
\echo '=== PLACES nearby 50km sem filtro (N11 pior caso) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT pl.id, ST_Distance(pl.location, ST_SetSRID(ST_MakePoint(-46.33, -23.25), 4326)::geography) AS distance,
  (SELECT COUNT(*) FROM check_ins c WHERE c.place_id = pl.id AND c.created_at > now() - interval '1 day') AS ci
FROM places pl LEFT JOIN partner_profiles pp ON pp.partner_id = pl.partner_id
WHERE ST_DWithin(pl.location, ST_SetSRID(ST_MakePoint(-46.33, -23.25), 4326)::geography, 50000)
ORDER BY distance ASC, pl.id LIMIT 21 OFFSET 0;
\echo '=== ACTIVITIES list (N2, owner com 100k) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT id FROM activities WHERE owner_id = :'heavy_owner' ORDER BY started_at DESC LIMIT 1000;
\echo '=== ACTIVITIES badgeStats COUNT/SUM por pet (N2) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT COUNT(*), COALESCE(SUM(distance_meters), 0) FROM activities WHERE pet_id = :'heavy_pet';
\echo '=== ACTIVITIES streak 400d (N2) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT DISTINCT started_at AT TIME ZONE 'UTC' AS d FROM activities WHERE pet_id = :'heavy_pet' AND started_at > now() - interval '400 days' ORDER BY d DESC;
\echo '=== OFFSET moderado comments page=900 size=50 (item com 50k comentários) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.id, o.display_name FROM comments c JOIN owners o ON o.id = c.owner_id WHERE c.feed_item_id = :'item' ORDER BY c.created_at ASC, c.id ASC LIMIT 51 OFFSET 45000;
\echo '=== OFFSET profundo comments page=10000 size=50 (N16, teto atual) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.id, o.display_name FROM comments c JOIN owners o ON o.id = c.owner_id WHERE c.feed_item_id = :'item' ORDER BY c.created_at ASC, c.id ASC LIMIT 51 OFFSET 500000;
\echo '=== OFFSET profundo comments page=2147483647 (N16, sem teto) ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT c.id, o.display_name FROM comments c JOIN owners o ON o.id = c.owner_id WHERE c.feed_item_id = :'item' ORDER BY c.created_at ASC, c.id ASC LIMIT 51 OFFSET 107374182350;
\echo '=== OFFSET profundo activities do pet heavy (PetProfile) page=10000 ==='
EXPLAIN (ANALYZE, BUFFERS)
SELECT a.id FROM activities a WHERE a.pet_id = :'heavy_pet' ORDER BY a.started_at DESC LIMIT 51 OFFSET 500000;
