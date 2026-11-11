package com.pawmotionai.migration

import org.flywaydb.core.Flyway
import org.junit.jupiter.api.Test
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.datasource.DriverManagerDataSource
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@Testcontainers
class LegacyDataMigrationIT {

    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"))
    }

    private val dataSource by lazy { DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password) }
    private val jdbc by lazy { JdbcTemplate(dataSource) }

    private fun flyway(target: String) =
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target(target).load()

    @Test
    fun `v6 data survives the PETLIFE migrations`() {
        flyway("6").migrate()
        seedV6()
        val ledgerBefore = count("SELECT COUNT(*) FROM ledger_entries") + count("SELECT COUNT(*) FROM ledger_accounts")

        flyway("latest").migrate()

        assertEquals(2, count("SELECT COUNT(*) FROM feed_items WHERE type = 'ACTIVITY'"))
        assertEquals(
            listOf("PAW:LIKE", "HEART:LOVE", "LAUGH:CHEER"),
            jdbc.queryForList("SELECT type || ':' || legacy_type FROM reactions ORDER BY created_at", String::class.java),
        )
        assertEquals(1, count("SELECT COUNT(*) FROM comments"))
        assertEquals(3, count("SELECT COUNT(*) FROM activity_reactions"), "legacy rows are kept until phase 6")
        assertEquals(3, count("SELECT COUNT(*) FROM xp_events"))
        assertTrue(count("SELECT COUNT(*) FROM pet_badges WHERE badge_code = 'FIRST_WALK'") == 1L)

        assertEquals(
            listOf("ACTIVE:", "ENDED:ARCHIVED", "ENDED:EXPIRED"),
            jdbc.queryForList("SELECT status || ':' || COALESCE(end_reason, '') FROM partner_campaigns ORDER BY title", String::class.java),
        )
        assertEquals(500, count("SELECT budget_credits FROM partner_campaigns WHERE title = 'A active'"))
        assertEquals(1, count("SELECT COUNT(*) FROM partner_profiles WHERE business_name = 'Paw Spa'"))
        assertEquals(1, count("SELECT COUNT(*) FROM places WHERE partner_id IS NOT NULL"))
        assertEquals(1, count("SELECT COUNT(*) FROM owners WHERE location IS NOT NULL AND role = 'PARTNER'"))
        assertEquals(1, count("SELECT COUNT(*) FROM ai_messages WHERE role = 'ASSISTANT'"))
        assertEquals(ledgerBefore, count("SELECT COUNT(*) FROM ledger_entries") + count("SELECT COUNT(*) FROM ledger_accounts"), "ledger untouched")
    }

    private fun count(sql: String): Long = jdbc.queryForObject(sql, Long::class.java)!!

    private fun seedV6() {
        jdbc.execute(
            """
            INSERT INTO owners (id, email, password_hash, display_name, role, status, created_at, updated_at)
            VALUES ('00000000-0000-0000-0000-000000000001', 'ana@example.com', 'x', 'Ana', 'OWNER', 'ACTIVE', now(), now()),
                   ('00000000-0000-0000-0000-000000000002', 'spa@example.com', 'x', 'Spa', 'PARTNER', 'ACTIVE', now(), now());
            UPDATE owners SET business_name = 'Paw Spa', website = 'https://paw.spa', latitude = -23.55, longitude = -46.63
            WHERE id = '00000000-0000-0000-0000-000000000002';
            INSERT INTO pets (id, owner_id, name, species, size, status, created_at, updated_at)
            VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000001', 'Thor', 'DOG', 'LARGE', 'ACTIVE', now(), now());
            INSERT INTO activities (id, owner_id, pet_id, type, started_at, duration_seconds, distance_meters, created_at, updated_at, share_to_paw_city)
            VALUES ('00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'WALK', now() - interval '2 day', 1800, 2400, now(), now(), TRUE),
                   ('00000000-0000-0000-0000-0000000000b2', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'RUN', now() - interval '1 day', 900, 3000, now(), now(), TRUE),
                   ('00000000-0000-0000-0000-0000000000b3', '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'PLAY', now(), 600, NULL, now(), now(), FALSE);
            INSERT INTO activity_reactions (id, activity_id, owner_id, type, created_at, updated_at)
            VALUES (gen_random_uuid(), '00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-000000000001', 'LIKE', now() - interval '3 min', now()),
                   (gen_random_uuid(), '00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-000000000002', 'LOVE', now() - interval '2 min', now()),
                   (gen_random_uuid(), '00000000-0000-0000-0000-0000000000b2', '00000000-0000-0000-0000-000000000001', 'CHEER', now() - interval '1 min', now());
            INSERT INTO activity_comments (id, activity_id, owner_id, body, created_at)
            VALUES (gen_random_uuid(), '00000000-0000-0000-0000-0000000000b1', '00000000-0000-0000-0000-000000000002', 'Good boy!', now());
            INSERT INTO partner_campaigns (id, partner_id, title, description, target_audience, cta, status, created_at, updated_at, credits_charged, expires_at)
            VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000002', 'A active', 'd', 'dogs', 'Book', 'ACTIVE', now(), now(), 500, now() + interval '30 day'),
                   (gen_random_uuid(), '00000000-0000-0000-0000-000000000002', 'B archived', 'd', 'dogs', 'Book', 'ARCHIVED', now(), now(), 500, now() + interval '30 day'),
                   (gen_random_uuid(), '00000000-0000-0000-0000-000000000002', 'C expired', 'd', 'dogs', 'Book', 'EXPIRED', now() - interval '40 day', now(), 500, now() - interval '10 day');
            INSERT INTO insights (id, owner_id, text, provider, fallback_used, generated_at, created_at, updated_at)
            VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', 'Great week!', 'local', TRUE, now(), now(), now());
            """.trimIndent(),
        )
    }
}
