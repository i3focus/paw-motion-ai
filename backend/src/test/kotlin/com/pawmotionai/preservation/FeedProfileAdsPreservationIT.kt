package com.pawmotionai.preservation

import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Preservation 3.5 (feed, comments, reactions of visible items, own profile, shared activities) and 3.6
 * (pause and resume without content change returns to ACTIVE, DRAFT editing stays free).
 */
class FeedProfileAdsPreservationIT : PreservationApiTest() {

    @Test
    fun `visible feed item accepts reactions and comments from a viewer in the same neighborhood`() {
        val author = registerOwner("Ana")
        val viewer = registerOwner("Bia")
        setNeighborhood(author.ownerId, "Pinheiros")
        setNeighborhood(viewer.ownerId, "Pinheiros")
        val pet = createPet(author)
        expect(201, recordActivity(author, pet, share = true))
        val itemId = expect(200, call("GET", "/api/feed?size=10", author.token))["items"][0]["id"].asString()

        val viewerItems = expect(200, call("GET", "/api/feed?size=50", viewer.token))["items"].elements()
        assertTrue(viewerItems.any { it["id"].asString() == itemId }, "item must be visible to the neighbor")

        assertEquals(200, call("PUT", "/api/feed/$itemId/reaction", viewer.token, mapOf("type" to "LAUGH")).status)
        assertEquals(200, call("DELETE", "/api/feed/$itemId/reaction", viewer.token).status)
        assertEquals(201, call("POST", "/api/feed/$itemId/comments", viewer.token, mapOf("body" to "so cute")).status)
        val comments = expect(200, call("GET", "/api/feed/$itemId/comments", viewer.token))
        assertTrue(comments.toString().contains("so cute"))
    }

    @Test
    fun `author sees and interacts with own feed item regardless of neighborhood`() {
        val author = registerOwner("Ana")
        val pet = createPet(author)
        expect(201, recordActivity(author, pet, share = true))
        val itemId = expect(200, call("GET", "/api/feed?size=10", author.token))["items"][0]["id"].asString()
        assertEquals(200, call("PUT", "/api/feed/$itemId/reaction", author.token, mapOf("type" to "LAUGH")).status)
        assertEquals(201, call("POST", "/api/feed/$itemId/comments", author.token, mapOf("body" to "my own")).status)
        assertEquals(200, call("GET", "/api/feed/$itemId/comments", author.token).status)
    }

    @Test
    fun `own profile counts every activity and a third party sees the shared ones`() {
        val owner = registerOwner("Ana")
        val other = registerOwner("Bia")
        val pet = createPet(owner)
        expect(201, recordActivity(owner, pet, share = true, notes = "shared note"))
        expect(201, recordActivity(owner, pet, share = false, notes = "private note"))

        val ownProfile = expect(200, call("GET", "/api/pets/$pet/profile", owner.token))
        assertEquals(2, ownProfile["stats"]["adventures"].asInt())
        val ownAdventures = call("GET", "/api/pets/$pet/adventures", owner.token)
        assertEquals(200, ownAdventures.status)
        assertTrue(ownAdventures.body.contains("shared note") && ownAdventures.body.contains("private note"), "owner sees all notes")

        // A third party always sees the shared activity; what the unfixed code also exposes about private ones is NOT pinned here (C19).
        val otherAdventures = call("GET", "/api/pets/$pet/adventures", other.token)
        assertEquals(200, otherAdventures.status)
        assertTrue(otherAdventures.body.contains("shared note"), "shared activity must stay visible to others")
        assertEquals(200, call("GET", "/api/pets/$pet/profile", other.token).status)
    }

    private fun draftCampaign(partner: Session): String {
        val created = expect(201, call("POST", "/api/partner/campaigns", partner.token, mapOf(
            "title" to "Spa day", "description" to "50% off baths", "targetAudience" to "dogs", "callToAction" to "Book now",
        )))
        assertEquals("DRAFT", created["status"].asString())
        return created["id"].asString()
    }

    private fun completeAndApprove(partner: Session, admin: Session, campaignId: String) {
        expect(200, call("PATCH", "/api/partner/campaigns/$campaignId", partner.token, mapOf(
            "mediaAssetId" to mediaAsset(partner.ownerId), "ctaType" to "LINK", "ctaUrl" to "https://happyspa.example.com",
            "formats" to listOf("FEED_CARD"),
        )))
        expect(200, call("POST", "/api/partner/campaigns/$campaignId/submit", partner.token))
        assertEquals("ACTIVE", expect(200, call("POST", "/api/admin/campaigns/$campaignId/approve", admin.token))["status"].asString())
    }

    @Test
    fun `pause then resume without changing content returns to ACTIVE, repeatedly`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner)
        val admin = registerOwner("Boss", role = "ADMIN")
        val campaignId = draftCampaign(partner)
        completeAndApprove(partner, admin, campaignId)
        repeat(3) {
            assertEquals("PAUSED", expect(200, call("POST", "/api/partner/campaigns/$campaignId/pause", partner.token))["status"].asString())
            assertEquals("ACTIVE", expect(200, call("POST", "/api/partner/campaigns/$campaignId/resume", partner.token))["status"].asString())
        }
        val ctaUrl = expect(200, call("GET", "/api/partner/campaigns/$campaignId", partner.token))["ctaUrl"].asString()
        assertEquals("https://happyspa.example.com", ctaUrl)
    }

    @Test
    fun `a campaign in DRAFT can be edited freely and stays in DRAFT`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner)
        val campaignId = draftCampaign(partner)
        val titles = listOf("First title", "Second title", "Third title ${UUID.randomUUID().toString().take(6)}")
        titles.forEach { title ->
            val edited = expect(200, call("PATCH", "/api/partner/campaigns/$campaignId", partner.token, mapOf(
                "title" to title, "ctaType" to "LINK", "ctaUrl" to "https://edit-${title.length}.example.com")))
            assertEquals("DRAFT", edited["status"].asString())
            assertEquals(title, edited["title"].asString())
        }
    }
}
