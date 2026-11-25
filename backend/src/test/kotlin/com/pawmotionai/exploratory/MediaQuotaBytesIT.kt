package com.pawmotionai.exploratory

import org.junit.jupiter.api.Test
import org.springframework.test.context.TestPropertySource
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals

/** PR 4b (C10): byte quota reserved atomically. Each test upload is 72 bytes; the cap allows exactly 10. Validates: Requirements 2.18. */
@TestPropertySource(properties = ["app.media.root=build/tmp/media-quota-bytes-it", "app.media.max-bytes-per-owner=720"])
class MediaQuotaBytesIT : ExploratoryApiTest() {

    @Test
    fun `C10 concurrent uploads never exceed the per-owner byte quota`() {
        val owner = registerOwner("Ana")
        val pool = Executors.newFixedThreadPool(16)
        val statuses = try {
            pool.invokeAll((1..30).map { i -> Callable { uploadPng(owner.token, i).status } }).map { it.get() }
        } finally {
            pool.shutdown()
        }
        assertEquals(10, statuses.count { it == 201 }, "statuses=${statuses.groupingBy { it }.eachCount()}")
        assertEquals(20, statuses.count { it == 409 })
        assertEquals(720L, jdbc.queryForObject("SELECT bytes FROM owner_media_usage WHERE owner_id = ?", Long::class.java, owner.ownerId))
        assertEquals(10L, jdbc.queryForObject("SELECT COUNT(*) FROM media_assets WHERE owner_id = ?", Long::class.java, owner.ownerId))
    }
}
