package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Lote 5 (C22): data integrity and XP. Spec: security-audit-remediation, task 1. */
@Exploratory
class Lote5ExploratoryIT : ExploratoryApiTest() {

    // ---------------- C22 ----------------

    private fun activeCampaign(partner: Session, formats: String = "{FEED_CARD}"): UUID {
        val id = UUID.randomUUID()
        jdbc.update(
            """
            INSERT INTO partner_campaigns (id, partner_id, title, description, target_audience, cta, status, created_at, updated_at,
                credits_charged, expires_at, starts_at, ends_at, target_species, formats)
            VALUES (?, ?, 'Spa', 'Spa day', 'dogs', 'Book now', 'ACTIVE', now(), now(), 500,
                now() + interval '30 day', now() - interval '1 day', now() + interval '30 day', '{}', CAST(? AS varchar(32)[]))
            """.trimIndent(),
            id, partner.ownerId, formats,
        )
        return id
    }

    @Test
    fun `C22 repeated CLICK by the same viewer is recorded once`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        val viewer = registerOwner("Bia")
        val campaign = activeCampaign(partner)
        repeat(5) { call("POST", "/api/ads/$campaign/events", viewer.token, mapOf("type" to "CLICK", "placement" to "FEED_CARD")) }
        val clicks = jdbc.queryForObject("SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ? AND type = 'CLICK'", Long::class.java, campaign)
        assertEquals(1L, clicks, "Counterexample C22: 5 CLICKs from one viewer -> $clicks recorded")
    }

    @Test
    fun `C22 placement outside the campaign formats is refused`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        val viewer = registerOwner("Bia")
        val campaign = activeCampaign(partner, formats = "{FEED_CARD}")
        val response = call("POST", "/api/ads/$campaign/events", viewer.token, mapOf("type" to "IMPRESSION", "placement" to "MAP_PIN"))
        val rows = jdbc.queryForObject("SELECT COUNT(*) FROM campaign_events WHERE campaign_id = ? AND placement = 'MAP_PIN'", Long::class.java, campaign)
        assertTrue(response.status in 400..499 && rows == 0L,
            "Counterexample C22: MAP_PIN event on a FEED_CARD-only campaign -> HTTP ${response.status}, rows recorded=$rows")
    }
}
