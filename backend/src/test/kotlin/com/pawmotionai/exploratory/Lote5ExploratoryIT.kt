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

/** Lote 5 (C6, C22): data integrity and XP. Spec: security-audit-remediation, task 1. */
@Exploratory
class Lote5ExploratoryIT : ExploratoryApiTest() {

    @Test
    fun `C6 daily XP per pet and source is capped`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(30) { i -> call("POST", "/api/activities", owner.token, mapOf(
            "petId" to pet, "type" to "walk", "startedAt" to Instant.now().minusSeconds(120L + i).toString(),
            "durationSeconds" to 60, "distanceMeters" to 0,
        )) }
        val xp = jdbc.queryForObject(
            "SELECT COALESCE(SUM(points), 0) FROM xp_events WHERE pet_id = ? AND source_type = 'ACTIVITY'", Long::class.java, pet)!!
        assertTrue(xp <= ProvisionalLimits.DAILY_XP_CAP_PER_PET_AND_SOURCE,
            "Counterexample C6: 30 activities in one day -> $xp ACTIVITY XP (cap ${ProvisionalLimits.DAILY_XP_CAP_PER_PET_AND_SOURCE})")
    }

    @Test
    fun `C6 playdate between two pets of the same owner awards no XP`() {
        val owner = registerOwner("Ana")
        val thor = createPet(owner, "Thor")
        val rex = createPet(owner, "Rex")
        val playdate = expect(201, call("POST", "/api/playdates", owner.token, mapOf(
            "petId" to thor, "mode" to "NOW", "inviteePetIds" to listOf(rex))))["id"].asString()
        call("POST", "/api/playdates/$playdate/respond", owner.token, mapOf("petId" to rex, "response" to "ACCEPTED"))
        val xp = jdbc.queryForObject(
            "SELECT COALESCE(SUM(points), 0) FROM xp_events WHERE owner_id = ? AND source_type = 'PLAYDATE'", Long::class.java, owner.ownerId)!!
        assertEquals(0L, xp, "Counterexample C6: self-playdate (Thor invites Rex, same owner) awarded $xp PLAYDATE XP")
    }

    @Test
    fun `C6 challenge reward is paid once per owner not once per pet`() {
        val admin = registerOwner("Boss", role = "ADMIN")
        val owner = registerOwner("Ana")
        val thor = createPet(owner, "Thor")
        val rex = createPet(owner, "Rex")
        val today = LocalDate.now(ZoneOffset.UTC)
        val challenge = expect(201, call("POST", "/api/admin/challenges", admin.token, mapOf(
            "title" to "Walk once ${UUID.randomUUID().toString().take(6)}", "description" to "one walk", "metric" to "ACTIVITY_COUNT",
            "targetValue" to 1, "xpReward" to 500, "startsOn" to today.minusDays(1).toString(), "endsOn" to today.plusDays(3).toString(),
        )))["id"].asString()
        listOf(thor, rex).forEach { expect(200, call("POST", "/api/engagement/challenges/$challenge/join", owner.token, mapOf("petId" to it))) }
        listOf(thor, rex).forEach { expect(201, recordActivity(owner, it)) }
        val rewards = jdbc.queryForObject(
            "SELECT COUNT(*) FROM xp_events WHERE owner_id = ? AND source_type = 'CHALLENGE' AND source_key LIKE ?",
            Long::class.java, owner.ownerId, "challenge:$challenge:%")
        assertEquals(1L, rewards, "Counterexample C6: one owner with two pets collected $rewards rewards for the same challenge")
    }

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
