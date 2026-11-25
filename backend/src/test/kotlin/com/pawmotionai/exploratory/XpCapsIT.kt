package com.pawmotionai.exploratory

import com.pawmotionai.engagement.XpReevaluationReport
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.random.Random
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PR 5b (C6): XP cap per (pet, source, day), no XP for same-owner playdates, reward once per owner. Validates: Requirements 2.22, 3.10. */
class XpCapsIT : ExploratoryApiTest() {
    @Autowired
    private lateinit var report: XpReevaluationReport

    private val cap = ProvisionalLimits.DAILY_XP_CAP_PER_PET_AND_SOURCE.toLong()

    private fun xp(petId: UUID, type: String) = jdbc.queryForObject(
        "SELECT COALESCE(SUM(points), 0) FROM xp_events WHERE pet_id = ? AND source_type = ?", Long::class.java, petId, type)!!

    @Test
    fun `C6 daily XP per pet and source is capped`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        repeat(30) { i -> recordActivity(owner, pet, Instant.now().minusSeconds(120L + i), 60, 0.0) }
        val xp = xp(pet, "ACTIVITY")
        assertEquals(cap, xp, "Counterexample C6: 30 activities in one day -> $xp ACTIVITY XP (cap $cap)")
    }

    @Test
    fun `C6 concurrent random activities never exceed the cap and legit XP below the cap is kept`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        val random = Random(42)
        val pool = Executors.newFixedThreadPool(8)
        try {
            pool.invokeAll((1..24).map { i ->
                val distance = random.nextDouble(0.0, 12_000.0)
                Callable { recordActivity(owner, pet, Instant.now().minusSeconds(60L + i), 60, distance).status }
            }).forEach { assertEquals(201, it.get()) }
        } finally {
            pool.shutdown()
        }
        assertTrue(xp(pet, "ACTIVITY") <= cap, "ACTIVITY XP ${xp(pet, "ACTIVITY")} exceeds cap $cap")
        val other = createPet(registerOwner("Bia").also { recordActivity(it, createPet(it), distanceMeters = 0.0) })
        assertEquals(0L, xp(other, "ACTIVITY"))
    }

    @Test
    fun `C6 a single legitimate activity below the cap keeps its full XP`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        expect(201, recordActivity(owner, pet, distanceMeters = 2000.0))
        assertEquals(20L, xp(pet, "ACTIVITY"))
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
    fun `C6 playdate with a pet of another owner still awards XP to both`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val thor = createPet(ana, "Thor")
        val rex = createPet(bia, "Rex")
        val playdate = expect(201, call("POST", "/api/playdates", ana.token, mapOf(
            "petId" to thor, "mode" to "NOW", "inviteePetIds" to listOf(rex))))["id"].asString()
        expect(200, call("POST", "/api/playdates/$playdate/respond", bia.token, mapOf("petId" to rex, "response" to "ACCEPTED")))
        assertEquals(20L, xp(thor, "PLAYDATE"))
        assertEquals(20L, xp(rex, "PLAYDATE"))
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

    @Test
    fun `C6 reevaluation report is dry-run and never removes xp events`() {
        val owner = registerOwner("Ana")
        val pet = createPet(owner)
        jdbc.update("INSERT INTO xp_events (id, pet_id, owner_id, source_type, source_key, points, occurred_at) VALUES (?, ?, ?, 'POST', 'legacy:a', 400, now())",
            UUID.randomUUID(), pet, owner.ownerId)
        val before = jdbc.queryForObject("SELECT COUNT(*) FROM xp_events", Long::class.java)
        val result = report.report()
        assertTrue(result.petDaysOverCap >= 1 && result.excessOverCapPoints >= 250)
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM xp_events", Long::class.java))
        assertEquals(400L, xp(pet, "POST"))
    }
}
