package com.pawmotionai.ads

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AdsApiIT : ApiIntegrationTest() {

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
}
