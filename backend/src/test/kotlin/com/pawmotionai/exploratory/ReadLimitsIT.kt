package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** PR 2b (C16; N11, N16): former exploratory tests, now regular. Validates: Requirements 2.11, 2.20. */
class ReadLimitsIT : ExploratoryApiTest() {

    @Test
    fun `C16 absurd page numbers are rejected with 400`() {
        val owner = registerOwner("Ana")
        val feed = call("GET", "/api/feed?page=100000000&size=50", owner.token).status
        assertEquals(400, feed, "Counterexample C16/N16: GET /api/feed?page=100000000 -> HTTP $feed (offset scan)")
    }

    @Test
    fun `C16 wallet entries page size is limited`() {
        val partner = registerOwner("Spa", role = "PARTNER")
        fundPartner(partner)
        val wallet = expect(200, call("GET", "/api/partner/wallet", partner.token))
        val accountId = wallet["accounts"][0]["id"].asString()
        val size = call("GET", "/api/partner/wallet/accounts/$accountId/entries?size=1000000", partner.token)
        val pageSize = runCatching { json(size)["size"].asInt() }.getOrDefault(-1)
        assertTrue(size.status == 400 || (size.status == 200 && pageSize in 1..50),
            "Counterexample N11: wallet entries size=1000000 -> HTTP ${size.status}, effective size=$pageSize")
    }
}
