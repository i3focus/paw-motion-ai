package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertFalse

/** Lote 1 (C15, C7, C19, C4): authorization and moderation. Spec: security-audit-remediation, task 1. */
@Exploratory
class Lote1ExploratoryIT : ExploratoryApiTest() {

    /** C15: a JWT claiming ADMIN for an account whose database role is OWNER must not open /api/admin/.... */
    @Test
    fun `C15 forged ADMIN claim for a non-admin account receives 403`() {
        val owner = registerOwner("Mallory", role = "OWNER")
        val forged = mintToken(owner.ownerId, listOf("ADMIN"))
        val response = call("GET", "/api/admin/campaigns", forged)
        assertEquals(403, response.status, "Counterexample C15: token roles=[ADMIN], db role=OWNER -> HTTP ${response.status}")
    }

    /** C15: after the ADMIN role is revoked in the database, the old token must lose access on the next request. */
    @Test
    fun `C15 revoked admin loses access on the next request with the old token`() {
        val admin = registerOwner("Boss", role = "ADMIN")
        assertEquals(200, call("GET", "/api/admin/campaigns", admin.token).status)
        jdbc.update("UPDATE owners SET role = 'OWNER' WHERE id = ?", admin.ownerId)
        val after = call("GET", "/api/admin/campaigns", admin.token)
        assertEquals(403, after.status, "Counterexample C15: role revoked in DB, old token still returns HTTP ${after.status}")
    }

    /** C7: user C outside the visibility scope must get 404 on react, unreact, list and create comments. */
    @Test
    fun `C7 feed item outside the viewer scope is 404 on react unreact comments and comment`() {
        val a = registerOwner("Ana")
        val c = registerOwner("Carla")
        setNeighborhood(a.ownerId, "Pinheiros")
        setNeighborhood(c.ownerId, "Moema")
        val pet = createPet(a)
        recordActivity(a, pet, share = true).also { expect(201, it) }
        val itemId = expect(200, call("GET", "/api/feed?size=5", a.token))["items"][0]["id"].asString()
        // Sanity: the scope really excludes C.
        assertEquals(0, expect(200, call("GET", "/api/feed?size=50", c.token))["items"].elements().count { it["id"].asString() == itemId })

        val statuses = linkedMapOf(
            "PUT reaction" to call("PUT", "/api/feed/$itemId/reaction", c.token, mapOf("type" to "LAUGH")).status,
            "DELETE reaction" to call("DELETE", "/api/feed/$itemId/reaction", c.token).status,
            "GET comments" to call("GET", "/api/feed/$itemId/comments", c.token).status,
            "POST comment" to call("POST", "/api/feed/$itemId/comments", c.token, mapOf("body" to "hi from outside")).status,
        )
        assertEquals(
            mapOf("PUT reaction" to 404, "DELETE reaction" to 404, "GET comments" to 404, "POST comment" to 404), statuses,
            "Counterexample C7: viewer C outside scope got $statuses",
        )
    }

    /** C19: a third party must not see notes or counts of activities that were not shared. */
    @Test
    fun `C19 adventures and profile of another owner hide non-shared activities`() {
        val a = registerOwner("Ana")
        val b = registerOwner("Bia")
        val pet = createPet(a)
        val secret = "PRIVATE-NOTE-${UUID.randomUUID()}"
        expect(201, recordActivity(a, pet, share = false, notes = secret))

        val adventures = call("GET", "/api/pets/$pet/adventures", b.token)
        val profile = expect(200, call("GET", "/api/pets/$pet/profile", b.token))
        val ownView = expect(200, call("GET", "/api/pets/$pet/profile", a.token))
        assertEquals(1, ownView["stats"]["adventures"].asInt(), "owner must still see own data")
        assertFalse(adventures.body.contains(secret), "Counterexample C19: B read A's private note from /adventures: ${adventures.body.take(200)}")
        assertEquals(0, profile["stats"]["adventures"].asInt(), "Counterexample C19: stats.adventures counts a private activity for B")
    }

    /** C4: approve, pause, edit ctaUrl, resume must not leave the campaign ACTIVE without a new review. */
    @Test
    fun `C4 campaign edited while paused is not ACTIVE after resume`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner)
        val admin = registerOwner("Boss", role = "ADMIN")
        val campaignId = expect(201, call("POST", "/api/partner/campaigns", partner.token, mapOf(
            "title" to "Spa day", "description" to "50% off baths", "targetAudience" to "dogs", "callToAction" to "Book now",
        )))["id"].asString()
        expect(200, call("PATCH", "/api/partner/campaigns/$campaignId", partner.token, mapOf(
            "mediaAssetId" to mediaAsset(partner.ownerId), "ctaType" to "LINK", "ctaUrl" to "https://happyspa.example.com",
            "formats" to listOf("FEED_CARD"),
        )))
        expect(200, call("POST", "/api/partner/campaigns/$campaignId/submit", partner.token))
        assertEquals("ACTIVE", expect(200, call("POST", "/api/admin/campaigns/$campaignId/approve", admin.token))["status"].asString())
        expect(200, call("POST", "/api/partner/campaigns/$campaignId/pause", partner.token))
        val patch = call("PATCH", "/api/partner/campaigns/$campaignId", partner.token, mapOf("ctaUrl" to "https://malicious.example.com/phish"))
        val resume = call("POST", "/api/partner/campaigns/$campaignId/resume", partner.token)
        val status = runCatching { json(resume)["status"].asString() }.getOrDefault("HTTP ${resume.status}")
        assertEquals(true, status != "ACTIVE", "Counterexample C4: patch while PAUSED -> ${patch.status}; resume -> status=$status (unreviewed ctaUrl is being served)")
    }
}
