package com.pawmotionai.media

import com.pawmotionai.feed.api.MediaDto
import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.MediaUrls
import org.springframework.core.io.Resource
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import java.time.LocalDate
import java.util.UUID

data class StoredMedia(val resource: Resource, val contentType: String)

@Service
class MediaService(private val jdbc: JdbcClient, private val storage: MediaStoragePort) {

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
        storage.store(key, bytes)
        jdbc.sql(
            "INSERT INTO media_assets (id, owner_id, storage_key, content_type, size_bytes, created_at) VALUES (:id, :owner, :key, :type, :size, now())",
        ).param("id", id).param("owner", ownerId).param("key", key).param("type", contentType).param("size", bytes.size.toLong()).update()
        return MediaDto(id, MediaUrls.of(id), contentType)
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
