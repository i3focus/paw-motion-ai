package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import java.util.UUID
import kotlin.test.assertEquals

/** PR 3b (C9): invite deduplication per organizer/owner window and daily cap. Validates: Requirements 2.14, 3.8. */
@TestPropertySource(properties = ["app.playdate.max-invite-notifications-per-day=3"])
class PlaydateInviteDedupIT : ExploratoryApiTest() {
    private fun body(petId: UUID, vararg invitees: UUID) = mapOf(
        "petId" to petId, "mode" to "NOW", "inviteePetIds" to invitees.toList(), "message" to "Let's play",
    )

    @Test
    fun `C9 two identical invites to the same owner within the window notify once`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val thor = createPet(ana, "Thor")
        val luna = createPet(bia, "Luna")
        repeat(2) { expect(201, call("POST", "/api/playdates", ana.token, body(thor, luna))) }
        assertEquals(1L, notificationCount(bia.ownerId, "Playdate invite"))
    }

    @Test
    fun `C9 two pets of the same owner in one request notify once`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val thor = createPet(ana, "Thor")
        val luna = createPet(bia, "Luna")
        val mel = createPet(bia, "Mel")
        expect(201, call("POST", "/api/playdates", ana.token, body(thor, luna, mel)))
        assertEquals(1L, notificationCount(bia.ownerId, "Playdate invite"))
    }

    @Test
    fun `C9 an invite to a pet of the same owner does not notify`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        val rex = createPet(ana, "Rex")
        expect(201, call("POST", "/api/playdates", ana.token, body(thor, rex)))
        assertEquals(0L, notificationCount(ana.ownerId, "Playdate invite"))
    }

    @Test
    fun `C9 invites outside the dedup window notify again`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")
        val thor = createPet(ana, "Thor")
        val luna = createPet(bia, "Luna")
        expect(201, call("POST", "/api/playdates", ana.token, body(thor, luna)))
        jdbc.update("UPDATE playdate_invites SET created_at = now() - interval '25 hours'")
        expect(201, call("POST", "/api/playdates", ana.token, body(thor, luna)))
        assertEquals(2L, notificationCount(bia.ownerId, "Playdate invite"))
    }

    @Test
    fun `C9 different organizers inviting the same owner both notify`() {
        val ana = registerOwner("Ana")
        val caio = registerOwner("Caio")
        val bia = registerOwner("Bia")
        val luna = createPet(bia, "Luna")
        expect(201, call("POST", "/api/playdates", ana.token, body(createPet(ana, "Thor"), luna)))
        expect(201, call("POST", "/api/playdates", caio.token, body(createPet(caio, "Bob"), luna)))
        assertEquals(2L, notificationCount(bia.ownerId, "Playdate invite"))
    }

    @Test
    fun `C9 daily cap refuses further notifying invites with 409`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        val statuses = (1..5).map {
            val owner = registerOwner("Dono$it")
            call("POST", "/api/playdates", ana.token, body(thor, createPet(owner, "Pet$it"))).status
        }
        assertEquals(listOf(201, 201, 201, 409, 409), statuses)
    }
}
