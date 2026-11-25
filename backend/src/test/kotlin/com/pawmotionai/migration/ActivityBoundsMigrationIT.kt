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
import kotlin.test.assertFalse

/** PR 5a: V27 adds NOT VALID checks and does not block or change legacy extreme activities. */
@Testcontainers
class ActivityBoundsMigrationIT {
    companion object {
        @Container
        @JvmStatic
        val postgres = PostgreSQLContainer(DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"))
    }

    private val dataSource by lazy { DriverManagerDataSource(postgres.jdbcUrl, postgres.username, postgres.password) }
    private val jdbc by lazy { JdbcTemplate(dataSource) }

    @Test
    fun `v27 keeps legacy extreme activities and rejects new ones`() {
        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("26").load().migrate()
        jdbc.execute(
            """
            INSERT INTO owners (id, email, password_hash, display_name, role, status, created_at, updated_at)
            VALUES ('00000000-0000-0000-0000-000000000001', 'ana@example.com', 'x', 'Ana', 'OWNER', 'ACTIVE', now(), now());
            INSERT INTO pets (id, owner_id, name, species, size, status, created_at, updated_at)
            VALUES ('00000000-0000-0000-0000-0000000000a1', '00000000-0000-0000-0000-000000000001', 'Thor', 'DOG', 'LARGE', 'ACTIVE', now(), now());
            INSERT INTO activities (id, owner_id, pet_id, type, started_at, duration_seconds, distance_meters, created_at, updated_at)
            VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'WALK',
                    '1970-01-01T00:00:00Z', 9000000000, 1e15, now(), now());
            """.trimIndent())

        Flyway.configure().dataSource(dataSource).locations("classpath:db/migration").target("latest").load().migrate()

        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM activities WHERE duration_seconds = 9000000000", Long::class.java))
        assertEquals(3L, jdbc.queryForObject(
            "SELECT COUNT(*) FROM pg_constraint WHERE conname LIKE 'chk_activities_%' AND NOT convalidated", Long::class.java))
        val rejected = runCatching {
            jdbc.update("INSERT INTO activities (id, owner_id, pet_id, type, started_at, duration_seconds, distance_meters, created_at, updated_at) " +
                "VALUES (gen_random_uuid(), '00000000-0000-0000-0000-000000000001', '00000000-0000-0000-0000-0000000000a1', 'WALK', now(), 10, 1e15, now(), now())")
        }
        assertFalse(rejected.isSuccess)
    }
}
