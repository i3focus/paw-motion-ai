-- Volume sintético para validação 11.1 (N1/N2/N11/N16). Somente local; nunca em produção.
SET synchronous_commit = off;
INSERT INTO owners (id, email, password_hash, display_name, role, status, created_at, updated_at, neighborhood, city, location, plan)
SELECT gen_random_uuid(), 'u'||g||'@x.test', 'x', 'Owner '||g, 'OWNER', 'ACTIVE', now(), now(),
       'Bairro '||(g % 200), 'SP',
       ST_SetSRID(ST_MakePoint(-46.63 + random()*0.6, -23.55 + random()*0.6), 4326)::geography, 'FREE'
FROM generate_series(1, 20000) g;
INSERT INTO pets (id, owner_id, name, species, size, status, created_at, updated_at)
SELECT gen_random_uuid(), o.id, 'Pet '||n, CASE n%2 WHEN 0 THEN 'DOG' ELSE 'CAT' END, 'MEDIUM', 'ACTIVE', now(), now()
FROM (SELECT id, row_number() over () n FROM owners) o;
-- 200k xp_events, 60% na última semana
INSERT INTO xp_events (id, pet_id, owner_id, source_type, source_key, points, occurred_at)
SELECT gen_random_uuid(), p.id, p.owner_id, 'ACTIVITY', 'k'||g, 10,
       now() - (CASE WHEN random() < 0.6 THEN random()*6.9 ELSE 7 + random()*300 END) * interval '1 day'
FROM generate_series(1, 200000) g
JOIN LATERAL (SELECT id, owner_id FROM pets OFFSET (g::bigint*7919) % 20000 LIMIT 1) p ON true;
-- 150k feed_items, 70% nos últimos 14 dias
INSERT INTO feed_items (id, type, ref_id, author_owner_id, author_pet_id, location, published_at)
SELECT gen_random_uuid(), 'POST', gen_random_uuid(), p.owner_id, p.id,
       CASE WHEN g % 3 = 0 THEN NULL ELSE ST_SetSRID(ST_MakePoint(-46.63 + random()*0.6, -23.55 + random()*0.6), 4326)::geography END,
       now() - (CASE WHEN random() < 0.7 THEN random()*13.9 ELSE 14 + random()*200 END) * interval '1 day'
FROM generate_series(1, 150000) g
JOIN LATERAL (SELECT id, owner_id FROM pets OFFSET (g::bigint*104729) % 20000 LIMIT 1) p ON true;
INSERT INTO reactions (id, feed_item_id, owner_id, type, created_at, updated_at)
SELECT gen_random_uuid(), f.id, (SELECT id FROM owners OFFSET ((r::bigint*31)) % 20000 LIMIT 1), 'PAW', now(), now()
FROM (SELECT id, row_number() over () r FROM feed_items) f WHERE r % 2 = 0;
-- 100k comentários espalhados + 50k em um item "quente" (para o teste de OFFSET profundo)
INSERT INTO comments (id, feed_item_id, owner_id, body, created_at)
SELECT gen_random_uuid(), f.id, o.id, 'c', now() - g * interval '1 second'
FROM generate_series(1, 100000) g
JOIN LATERAL (SELECT id FROM feed_items OFFSET (g::bigint*37) % 150000 LIMIT 1) f ON true
JOIN LATERAL (SELECT id FROM owners OFFSET g % 20000 LIMIT 1) o ON true;
INSERT INTO comments (id, feed_item_id, owner_id, body, created_at)
SELECT gen_random_uuid(), (SELECT id FROM feed_items OFFSET 5 LIMIT 1), o.id, 'c', now() + g * interval '1 second'
FROM generate_series(1, 50000) g JOIN LATERAL (SELECT id FROM owners OFFSET g % 20000 LIMIT 1) o ON true;
INSERT INTO pet_follows (follower_owner_id, pet_id, created_at)
SELECT DISTINCT o.id, p.id, now() FROM generate_series(1,60000) g
JOIN LATERAL (SELECT id FROM owners OFFSET (g::bigint*13)%20000 LIMIT 1) o ON true
JOIN LATERAL (SELECT id FROM pets OFFSET (g::bigint*17)%20000 LIMIT 1) p ON true;
-- 100k places
INSERT INTO places (id, name, category, location, created_at, updated_at, rating, rating_count)
SELECT gen_random_uuid(), 'Place '||g||' '||md5(g::text), (ARRAY['PARK','VET','CAFE','HOTEL','GROOMING','PET_SHOP','TRAINING','OTHER'])[1 + g%8],
       ST_SetSRID(ST_MakePoint(-46.63 + random()*0.6, -23.55 + random()*0.6), 4326)::geography, now(), now(), 4, 10
FROM generate_series(1, 100000) g;
INSERT INTO check_ins (id, place_id, owner_id, pet_id, created_at)
SELECT gen_random_uuid(), pl.id, p.owner_id, p.id, now() - random()*interval '3 days'
FROM generate_series(1,100000) g
JOIN LATERAL (SELECT id FROM places OFFSET (g::bigint*11)%100000 LIMIT 1) pl ON true
JOIN LATERAL (SELECT id, owner_id FROM pets OFFSET (g::bigint*3)%20000 LIMIT 1) p ON true;
-- 20k comunidades, 150k membros, 40k eventos
INSERT INTO communities (id, slug, name, kind, description, created_at)
SELECT gen_random_uuid(), 'c-'||g, 'Comunidade '||g, (ARRAY['BREED','NEIGHBORHOOD','CAUSE'])[1+g%3], 'descricao '||g, now()
FROM generate_series(1,20000) g;
INSERT INTO community_members (community_id, owner_id, role, joined_at)
SELECT c.id, o.id, 'MEMBER', now()
FROM (SELECT id, r, g FROM (SELECT id, row_number() over () r FROM communities) x, generate_series(1,8) g) c
JOIN LATERAL (SELECT id FROM owners OFFSET (c.r::bigint*c.g)%20000 LIMIT 1) o ON true
ON CONFLICT DO NOTHING;
INSERT INTO community_events (id, community_id, title, starts_at, created_by, created_at)
SELECT gen_random_uuid(), c.id, 'Evento', now() + (g-1) * interval '2 days', (SELECT id FROM owners LIMIT 1), now()
FROM communities c, generate_series(1,2) g;
-- 200k activities: 1 owner abusivo com 100k (N2), resto distribuído
DO $$ DECLARE o uuid; p uuid; BEGIN
  SELECT owner_id, id INTO o, p FROM pets LIMIT 1;
  INSERT INTO activities (id, owner_id, pet_id, type, started_at, duration_seconds, distance_meters, created_at, updated_at)
  SELECT gen_random_uuid(), o, p, 'WALK', now() - g * interval '5 minutes', 600, 1000, now() - g * interval '5 minutes', now() FROM generate_series(1,100000) g;
END $$;
INSERT INTO activities (id, owner_id, pet_id, type, started_at, duration_seconds, distance_meters, created_at, updated_at)
SELECT gen_random_uuid(), p.owner_id, p.id, 'WALK', t, 600, 1000, t, now()
FROM (SELECT g, now() - random()*interval '300 days' AS t FROM generate_series(1,100000) g) s JOIN LATERAL (SELECT id, owner_id FROM pets OFFSET (s.g::bigint*7)%20000 LIMIT 1) p ON true;
ANALYZE;
