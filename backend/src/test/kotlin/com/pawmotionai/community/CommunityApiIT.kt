package com.pawmotionai.community

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.time.Instant
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommunityApiIT : ApiIntegrationTest() {

    @Test
    fun `community lifecycle join events rsvp and discover`() {
        val ana = registerOwner("Ana")
        val bia = registerOwner("Bia")

        val community = expect(201, call("POST", "/api/communities", ana.token, mapOf(
            "name" to "Golden Club", "kind" to "BREED", "description" to "Golden retrievers",
        )))
        val id = community["id"].asString()
        assertEquals(true, community["joined"].asBoolean())
        assertEquals("MODERATOR", community["role"].asString())

        val listed = expect(200, call("GET", "/api/communities?kind=BREED", bia.token))
        assertEquals(1, listed["items"].size())
        assertEquals(1, listed["items"][0]["memberCount"].asInt())

        val joined = expect(200, call("POST", "/api/communities/$id/join", bia.token))
        assertEquals(2, joined["memberCount"].asInt())

        val event = expect(201, call("POST", "/api/communities/$id/events", bia.token, mapOf(
            "title" to "Park meetup", "startsAt" to Instant.now().plusSeconds(7200).toString(), "locationName" to "City Park",
        )))
        expect(403, call("POST", "/api/communities/$id/events", registerOwner("Caio").token, mapOf(
            "title" to "Sneaky", "startsAt" to Instant.now().plusSeconds(7200).toString(),
        )))

        val rsvp = expect(200, call("POST", "/api/events/${event["id"].asString()}/rsvp", ana.token, mapOf("status" to "GOING")))
        assertEquals(1, rsvp["goingCount"].asInt())
        assertEquals("GOING", rsvp["viewerRsvp"].asString())

        val discover = expect(200, call("GET", "/api/discover", ana.token))
        assertTrue(discover["communities"].any { it["id"].asString() == id })
        assertTrue(discover.has("events") && discover.has("sponsored"))

        val left = expect(200, call("DELETE", "/api/communities/$id/join", bia.token))
        assertEquals(1, left["memberCount"].asInt())
    }

    @Test
    fun `community create validates input`() {
        val ana = registerOwner("Ana")
        expect(422, call("POST", "/api/communities", ana.token, mapOf("name" to "", "kind" to "BREED")))
        expect(401, call("GET", "/api/communities"))
    }
}
