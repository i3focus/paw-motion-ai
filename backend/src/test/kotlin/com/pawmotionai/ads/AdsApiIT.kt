package com.pawmotionai.ads

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import com.pawmotionai.feed.adapter.JdbcFeedRepository
import com.pawmotionai.feed.adapter.Viewer
import com.pawmotionai.feed.api.AdPlacement
import com.pawmotionai.feed.domain.ViewerProfile
import org.springframework.beans.factory.annotation.Autowired
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdsApiIT : ApiIntegrationTest() {
    @Autowired
    private lateinit var feedRepository: JdbcFeedRepository

    private fun status(id: String) = jdbc.queryForObject("SELECT status FROM partner_campaigns WHERE id = ?::uuid", String::class.java, id)
    private fun hash(id: String) = jdbc.queryForObject("SELECT campaign_content_hash(?::uuid)", String::class.java, id)
    private fun served(id: String, viewer: UUID): Boolean =
        feedRepository.sponsoredCandidates(Viewer(viewer, null, null, null, ViewerProfile(emptySet(), emptySet(), emptySet())), AdPlacement.FEED_CARD)
            .any { it.content.campaignId.toString() == id }


    private fun fund(partner: Session, credits: Long = 10000) {
        val wallet = UUID.randomUUID()
        jdbc.update("INSERT INTO ledger_wallets (id, partner_id, status, created_at, updated_at) VALUES (?, ?, 'ACTIVE', now(), now())", wallet, partner.ownerId)
        jdbc.update(
            "INSERT INTO ledger_accounts (id, wallet_id, code, debit_priority, status, balance, created_at, updated_at) VALUES (?, ?, 'MAIN', 1, 'OPEN', ?, now(), now())",
            UUID.randomUUID(), wallet, credits,
        )
    }

    private fun media(ownerId: UUID): UUID {
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO media_assets (id, owner_id, storage_key, content_type, size_bytes, created_at) VALUES (?, ?, ?, 'image/png', 1000, now())", id, ownerId, "test/$id.png")
        return id
    }

    @Test
    fun `campaign v2 lifecycle create patch submit approve pause resume end and metrics`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fund(partner)
        expect(200, call("PATCH", "/api/partner/profile", partner.token, mapOf(
            "businessName" to "Happy Spa", "category" to "GROOMING", "description" to "Baths and trims",
        )))
        assertEquals("Happy Spa", expect(200, call("GET", "/api/partner/profile", partner.token))["businessName"].asString())

        val created = expect(201, call("POST", "/api/partner/campaigns", partner.token, mapOf(
            "title" to "Spa day", "description" to "50% off baths", "targetAudience" to "dogs", "callToAction" to "Book now",
        )))
        val campaignId = created["id"].asString()
        assertEquals("DRAFT", created["status"].asString())

        expect(400, call("POST", "/api/partner/campaigns/$campaignId/submit", partner.token))
        expect(200, call("PATCH", "/api/partner/campaigns/$campaignId", partner.token, mapOf(
            "mediaAssetId" to media(partner.ownerId), "ctaType" to "LINK", "ctaUrl" to "https://happyspa.example.com",
            "formats" to listOf("FEED_CARD", "MAP_PIN"), "targetSpecies" to listOf("DOG"),
            "targetLatitude" to -23.56, "targetLongitude" to -46.65, "targetRadiusMeters" to 5000,
        )))
        val submitted = expect(200, call("POST", "/api/partner/campaigns/$campaignId/submit", partner.token))
        assertEquals("IN_REVIEW", submitted["status"].asString())

        val admin = registerOwner("Boss", role = "ADMIN")
        expect(403, call("GET", "/api/admin/campaigns", partner.token))
        val queue = expect(200, call("GET", "/api/admin/campaigns?status=IN_REVIEW", admin.token))
        assertEquals(1, queue["items"].size())
        val approved = expect(200, call("POST", "/api/admin/campaigns/$campaignId/approve", admin.token))
        assertEquals("ACTIVE", approved["status"].asString())

        expect(204, call("POST", "/api/ads/$campaignId/events", partner.token, mapOf("type" to "IMPRESSION", "placement" to "FEED_CARD")))
        expect(204, call("POST", "/api/ads/$campaignId/events", partner.token, mapOf("type" to "CLICK", "placement" to "FEED_CARD")))
        val metrics = expect(200, call("GET", "/api/partner/campaigns/$campaignId/metrics", partner.token))
        assertEquals(1, metrics["impressions"].asInt())
        assertEquals(1, metrics["clicks"].asInt())

        assertEquals("PAUSED", expect(200, call("POST", "/api/partner/campaigns/$campaignId/pause", partner.token))["status"].asString())
        assertEquals("ACTIVE", expect(200, call("POST", "/api/partner/campaigns/$campaignId/resume", partner.token))["status"].asString())
        assertEquals("ENDED", expect(200, call("POST", "/api/partner/campaigns/$campaignId/end", partner.token))["status"].asString())
        expect(409, call("POST", "/api/partner/campaigns/$campaignId/pause", partner.token))
    }

    @Test
    fun `campaign ownership review reject and partner verification`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fund(partner)
        val other = registerOwner("Rival", role = "PARTNER")
        fund(other)
        val created = expect(201, call("POST", "/api/partner/campaigns", partner.token, mapOf(
            "title" to "Sneaky", "description" to "x", "targetAudience" to "all", "callToAction" to "Go",
        )))
        val campaignId = created["id"].asString()

        expect(403, call("GET", "/api/partner/campaigns/$campaignId", other.token))
        expect(403, call("PATCH", "/api/partner/campaigns/$campaignId", other.token, mapOf("title" to "Hijack")))
        expect(403, call("GET", "/api/partner/campaigns/$campaignId/metrics", other.token))

        expect(200, call("PATCH", "/api/partner/campaigns/$campaignId", partner.token, mapOf(
            "mediaAssetId" to media(partner.ownerId), "ctaType" to "LINK", "ctaUrl" to "https://x.example.com",
        )))
        expect(200, call("POST", "/api/partner/campaigns/$campaignId/submit", partner.token))
        val admin = registerOwner("Boss", role = "ADMIN")
        val rejected = expect(200, call("POST", "/api/admin/campaigns/$campaignId/reject", admin.token, mapOf("reason" to "Inappropriate content")))
        assertEquals("DRAFT", rejected["status"].asString())
        assertEquals("Inappropriate content", rejected["rejectionReason"].asString())

        expect(200, call("PATCH", "/api/partner/profile", partner.token, mapOf("businessName" to "Happy Spa", "category" to "VET")))
        assertEquals(false, expect(200, call("GET", "/api/partner/profile", partner.token))["verified"].asBoolean())
        expect(200, call("POST", "/api/admin/partners/${partner.ownerId}/verify", admin.token))
        assertEquals(true, expect(200, call("GET", "/api/partner/profile", partner.token))["verified"].asBoolean())
    }

    @Test
    fun `approved content cycle approve pause edit resume reject and approve again`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fund(partner)
        val admin = registerOwner("Boss", role = "ADMIN")
        val id = expect(201, call("POST", "/api/partner/campaigns", partner.token, mapOf(
            "title" to "Spa day", "description" to "50% off", "targetAudience" to "dogs", "callToAction" to "Book",
        )))["id"].asString()
        expect(200, call("PATCH", "/api/partner/campaigns/$id", partner.token, mapOf(
            "mediaAssetId" to media(partner.ownerId), "ctaType" to "LINK", "ctaUrl" to "https://ok.example.com", "formats" to listOf("FEED_CARD"),
        )))
        expect(200, call("POST", "/api/partner/campaigns/$id/submit", partner.token))
        expect(200, call("POST", "/api/admin/campaigns/$id/approve", admin.token))
        assertTrue(served(id, partner.ownerId))
        // unchanged pause/resume stays ACTIVE
        expect(200, call("POST", "/api/partner/campaigns/$id/pause", partner.token))
        assertEquals("ACTIVE", expect(200, call("POST", "/api/partner/campaigns/$id/resume", partner.token))["status"].asString())
        // content edited while paused goes back to DRAFT and is not served
        expect(200, call("POST", "/api/partner/campaigns/$id/pause", partner.token))
        assertEquals("DRAFT", expect(200, call("PATCH", "/api/partner/campaigns/$id", partner.token, mapOf("ctaUrl" to "https://evil.example.com")))["status"].asString())
        expect(409, call("POST", "/api/partner/campaigns/$id/resume", partner.token))
        assertTrue(!served(id, partner.ownerId))
        // resubmit, reject, fix, resubmit, approve
        expect(200, call("POST", "/api/partner/campaigns/$id/submit", partner.token))
        expect(200, call("POST", "/api/admin/campaigns/$id/reject", admin.token, mapOf("reason" to "no")))
        expect(200, call("PATCH", "/api/partner/campaigns/$id", partner.token, mapOf("ctaUrl" to "https://ok.example.com")))
        expect(200, call("POST", "/api/partner/campaigns/$id/submit", partner.token))
        assertEquals("ACTIVE", expect(200, call("POST", "/api/admin/campaigns/$id/approve", admin.token))["status"].asString())
        assertTrue(served(id, partner.ownerId))
        // extending ends_at past the approved date also revokes approval
        expect(200, call("POST", "/api/partner/campaigns/$id/pause", partner.token))
        val later = java.time.Instant.now().plus(java.time.Duration.ofDays(400)).toString()
        assertEquals("DRAFT", expect(200, call("PATCH", "/api/partner/campaigns/$id", partner.token, mapOf("endsAt" to later)))["status"].asString())
    }

    @Test
    fun `legacy paused campaign without approved hash goes to review on resume`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fund(partner)
        val id = expect(201, call("POST", "/api/partner/campaigns", partner.token, mapOf(
            "title" to "Old", "description" to "d", "targetAudience" to "all", "callToAction" to "Go",
        )))["id"].asString()
        jdbc.update("UPDATE partner_campaigns SET status = 'PAUSED', approved_content_hash = NULL, approved_ends_at = NULL WHERE id = ?::uuid", id)
        assertEquals("IN_REVIEW", expect(200, call("POST", "/api/partner/campaigns/$id/resume", partner.token))["status"].asString())
        assertTrue(!served(id, partner.ownerId))
    }

    @Test
    fun `content hash is stable under array reordering and sensitive to each field`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        val id = expect(201, call("POST", "/api/partner/campaigns", partner.token, mapOf(
            "title" to "T", "description" to "D", "targetAudience" to "A", "callToAction" to "C",
        )))["id"].asString()
        jdbc.update("UPDATE partner_campaigns SET formats = '{FEED_CARD,MAP_PIN}', target_species = '{DOG,CAT}', target_breeds = '{a,b}', target_interests = '{x,y}' WHERE id = ?::uuid", id)
        val base = hash(id)
        jdbc.update("UPDATE partner_campaigns SET formats = '{MAP_PIN,FEED_CARD}', target_species = '{CAT,DOG}', target_breeds = '{b,a}', target_interests = '{y,x}' WHERE id = ?::uuid", id)
        assertEquals(base, hash(id))
        val mutations = listOf(
            "title = 'T2'", "description = 'D2'", "cta = 'C2'", "cta_type = 'BOOKING'", "cta_url = 'https://z.example.com'",
            "service_id = NULL, target_audience = 'A2'", "formats = '{MAP_PIN}'", "target_species = '{DOG}'", "target_breeds = '{a}'",
            "target_interests = '{x}'", "target_location = ST_SetSRID(ST_MakePoint(-46, -23), 4326)::geography", "target_radius_m = 1000",
        )
        mutations.forEach { set ->
            jdbc.update("UPDATE partner_campaigns SET $set WHERE id = ?::uuid", id)
            assertTrue(hash(id) != base, "hash must change for: $set")
            jdbc.update("UPDATE partner_campaigns SET title = 'T', description = 'D', cta = 'C', cta_type = 'LINK', cta_url = NULL, target_audience = 'A', formats = '{FEED_CARD,MAP_PIN}', target_species = '{DOG,CAT}', target_breeds = '{a,b}', target_interests = '{x,y}', target_location = NULL, target_radius_m = NULL WHERE id = ?::uuid", id)
        }
        val media = media(partner.ownerId)
        jdbc.update("UPDATE partner_campaigns SET media_asset_id = ? WHERE id = ?::uuid", media, id)
        assertTrue(hash(id) != base)
    }
}
