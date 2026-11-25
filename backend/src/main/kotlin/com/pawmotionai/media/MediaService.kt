package com.pawmotionai.media

import com.pawmotionai.feed.api.MediaDto
import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.ConflictException
import io.micrometer.core.instrument.Gauge
import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.transaction.support.TransactionTemplate
import com.pawmotionai.shared.MediaUrls
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

data class StoredMedia(val resource: Resource, val contentType: String)

@Service
class MediaService(
    private val jdbc: JdbcClient,
    private val storage: MediaStoragePort,
    private val tx: TransactionTemplate,
    private val meterRegistry: MeterRegistry,
    @Value("\${app.media.mode:enforce}") private val mode: String,
    @Value("\${app.media.max-bytes-per-owner:5368709120}") private val maxBytesPerOwner: Long,
    @Value("\${app.media.max-objects-per-owner:50}") private val maxObjectsPerOwner: Long,
) {
    private val log = LoggerFactory.getLogger(MediaService::class.java)

    init {
        Gauge.builder("media.owner_bytes_total") { scalar("SELECT COALESCE(SUM(bytes), 0) FROM owner_media_usage") }.register(meterRegistry)
        Gauge.builder("media.assets_table_bytes") { scalar("SELECT pg_total_relation_size('media_assets')") }.register(meterRegistry)
    }

    private fun scalar(sql: String): Double = runCatching { jdbc.sql(sql).query(Long::class.java).single().toDouble() }.getOrDefault(Double.NaN)

    companion object {
        const val MAX_BYTES = 10L * 1024 * 1024

        /** Detects the real type from magic bytes; the client-declared content type is not trusted. */
        fun detectContentType(bytes: ByteArray): String? {
            fun startsWith(vararg sig: Int) = bytes.size >= sig.size && sig.indices.all { bytes[it].toInt() and 0xFF == sig[it] }
            fun asciiAt(offset: Int, text: String) =
                bytes.size >= offset + text.length && text.indices.all { bytes[offset + it].toInt().toChar() == text[it] }
            return when {
                startsWith(0xFF, 0xD8, 0xFF) -> "image/jpeg"
                startsWith(0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) -> "image/png"
                asciiAt(0, "RIFF") && asciiAt(8, "WEBP") -> "image/webp"
                asciiAt(4, "ftyp") && (asciiAt(8, "heic") || asciiAt(8, "heix") || asciiAt(8, "mif1")) -> "image/heic"
                asciiAt(4, "ftyp") -> "video/mp4"
                else -> null
            }
        }

        private val EXTENSIONS = mapOf(
            "image/jpeg" to "jpg", "image/png" to "png", "image/webp" to "webp", "image/heic" to "heic", "video/mp4" to "mp4",
        )
    }

    fun upload(ownerId: UUID, bytes: ByteArray): MediaDto {
        if (bytes.isEmpty()) throw BadRequestException("File is empty")
        if (bytes.size > MAX_BYTES) throw BadRequestException("File exceeds 10 MB")
        val contentType = detectContentType(bytes) ?: throw BadRequestException("Only JPEG, PNG, WebP, HEIC images and MP4 videos are supported")
        val id = UUID.randomUUID()
        val key = "${LocalDate.now()}/$id.${EXTENSIONS.getValue(contentType)}"
        val size = bytes.size.toLong()
        // Reservation, row and file write share one transaction: a failure rolls the reservation back (C10).
        try {
            tx.executeWithoutResult {
                reserve(ownerId, size)
                jdbc.sql(
                    "INSERT INTO media_assets (id, owner_id, storage_key, content_type, size_bytes, created_at) VALUES (:id, :owner, :key, :type, :size, now())",
                ).param("id", id).param("owner", ownerId).param("key", key).param("type", contentType).param("size", size).update()
                storage.store(key, bytes)
            }
        } catch (e: Exception) {
            runCatching { storage.delete(key) }
            throw e
        }
        return MediaDto(id, MediaUrls.of(id), contentType)
    }

    /** Atomically reserves quota with a conditional UPDATE (row lock serializes uploads of one owner). */
    private fun reserve(ownerId: UUID, size: Long) {
        jdbc.sql("INSERT INTO owner_media_usage (owner_id, bytes, objects) VALUES (:owner, 0, 0) ON CONFLICT (owner_id) DO NOTHING")
            .param("owner", ownerId).update()
        if (mode == "off") {
            addUsage(ownerId, size)
            return
        }
        val reserved = jdbc.sql(
            "UPDATE owner_media_usage SET bytes = bytes + :size, objects = objects + 1 " +
                "WHERE owner_id = :owner AND bytes + :size <= :cap AND objects + 1 <= :capObjects",
        ).param("size", size).param("owner", ownerId).param("cap", maxBytesPerOwner).param("capObjects", maxObjectsPerOwner).update()
        if (reserved == 1) return
        meterRegistry.counter("media.quota_exceeded", "mode", mode).increment()
        if (mode == "shadow") {
            log.warn("media quota would be exceeded owner={}", ownerId)
            addUsage(ownerId, size)
            return
        }
        throw ConflictException("Media quota exceeded")
    }

    private fun addUsage(ownerId: UUID, size: Long) {
        jdbc.sql("UPDATE owner_media_usage SET bytes = bytes + :size, objects = objects + 1 WHERE owner_id = :owner")
            .param("size", size).param("owner", ownerId).update()
    }

    fun load(mediaId: UUID): StoredMedia? {
        val row = jdbc.sql("SELECT storage_key, content_type FROM media_assets WHERE id = :id").param("id", mediaId)
            .query { rs, _ -> rs.getString(1) to rs.getString(2) }.optional().orElse(null) ?: return null
        return storage.load(row.first)?.let { StoredMedia(it, row.second) }
    }

    /** True when every id is an upload owned by [ownerId]. */
    fun ownsAll(ownerId: UUID, mediaIds: Collection<UUID>): Boolean = mediaIds.isEmpty() ||
        jdbc.sql("SELECT COUNT(*) FROM media_assets WHERE owner_id = :owner AND id IN (:ids)")
            .param("owner", ownerId).param("ids", mediaIds.toSet()).query(Long::class.java).single() == mediaIds.toSet().size.toLong()
}
