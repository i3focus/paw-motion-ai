package com.pawmotionai.exploratory

import com.pawmotionai.media.MediaDeletionOutbox
import com.pawmotionai.media.MediaOrphanSweep
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.test.context.TestPropertySource
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.Callable
import java.util.concurrent.Executors
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** PR 4b (C10): atomic per-owner object quota (default 50), file deletion by outbox and dry-run orphan sweep. Validates: Requirements 2.18, 3.9, 3.13. */
@TestPropertySource(properties = ["app.media.root=build/tmp/media-quota-it"])
class MediaQuotaIT : ExploratoryApiTest() {

    @Autowired
    private lateinit var outbox: MediaDeletionOutbox

    @Autowired
    private lateinit var sweep: MediaOrphanSweep

    private val root: Path = Path.of("build/tmp/media-quota-it").toAbsolutePath()

    private fun upload(token: String, salt: Int) = uploadPng(token, salt)

    private fun concurrentUploads(token: String, total: Int): List<Int> {
        val pool = Executors.newFixedThreadPool(16)
        return try {
            pool.invokeAll((1..total).map { i -> Callable { upload(token, i).status } }).map { it.get() }
        } finally {
            pool.shutdown()
        }
    }

    @Test
    fun `C10 concurrent uploads never exceed the per-owner object quota`() {
        val owner = registerOwner("Ana")
        val statuses = concurrentUploads(owner.token, ProvisionalLimits.MAX_MEDIA_OBJECTS_PER_OWNER + 10)
        val stored = jdbc.queryForObject("SELECT COUNT(*) FROM media_assets WHERE owner_id = ?", Long::class.java, owner.ownerId)!!
        assertEquals(ProvisionalLimits.MAX_MEDIA_OBJECTS_PER_OWNER.toLong(), stored, "statuses=${statuses.groupingBy { it }.eachCount()}")
        assertEquals(10, statuses.count { it == 409 })
        val usage = jdbc.queryForMap("SELECT bytes, objects FROM owner_media_usage WHERE owner_id = ?", owner.ownerId)
        assertEquals(stored, usage["objects"])
        assertEquals(stored * 72, usage["bytes"])
        // another owner is not affected
        assertEquals(201, upload(registerOwner("Bia").token, 1).status)
    }

    @Test
    fun `C10 deleting a media row releases the quota and removes the file after the outbox drain`() {
        val owner = registerOwner("Caio")
        val body = mapper.readTree(upload(owner.token, 7).also { assertEquals(201, it.status) }.body)
        val id = body["id"].asText()
        val key = jdbc.queryForObject("SELECT storage_key FROM media_assets WHERE id = ?::uuid", String::class.java, id)!!
        assertTrue(Files.exists(root.resolve(key)))

        jdbc.update("DELETE FROM media_assets WHERE id = ?::uuid", id)
        assertTrue(Files.exists(root.resolve(key)), "file must only be removed after the outbox drain")
        assertEquals(0L, jdbc.queryForObject("SELECT objects FROM owner_media_usage WHERE owner_id = ?", Long::class.java, owner.ownerId))

        outbox.drain()
        assertFalse(Files.exists(root.resolve(key)))
        assertEquals(1L, jdbc.queryForObject("SELECT COUNT(*) FROM media_deletions WHERE storage_key = ? AND processed_at IS NOT NULL", Long::class.java, key))
    }

    @Test
    fun `C10 orphan sweep reports in dry-run mode and never deletes files or rows`() {
        val owner = registerOwner("Duda")
        upload(owner.token, 3)
        val orphan = root.resolve("orphans/lost.png")
        Files.createDirectories(orphan.parent)
        Files.write(orphan, byteArrayOf(1, 2, 3))
        Files.setLastModifiedTime(orphan, java.nio.file.attribute.FileTime.fromMillis(System.currentTimeMillis() - 7_200_000))
        val rowsBefore = jdbc.queryForObject("SELECT COUNT(*) FROM media_assets", Long::class.java)

        val report = sweep.sweep()

        assertTrue(report.orphanFiles >= 1)
        assertTrue(Files.exists(orphan))
        assertEquals(rowsBefore, jdbc.queryForObject("SELECT COUNT(*) FROM media_assets", Long::class.java))
    }
}
