package com.pawmotionai.preservation

import com.pawmotionai.support.ApiIntegrationTest
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.UUID

/**
 * Base for the preservation tests (spec `security-audit-remediation`, task 2, Property 2: Preservation).
 *
 * These tests pin behavior observed on the UNFIXED code for inputs where no `isBugCondition_Cn` holds. They must pass
 * before and after every remediation PR and they run in the default CI (they are not tagged `exploratory`).
 */
abstract class PreservationApiTest : ApiIntegrationTest() {

    private val rawHttp: HttpClient = HttpClient.newHttpClient()

    /** Sends [rawBody] as-is, with extra headers (webhooks, idempotency keys). */
    protected fun rawCall(
        method: String,
        path: String,
        token: String?,
        rawBody: String,
        headers: Map<String, String> = emptyMap(),
    ): Response {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:${environment.getProperty("local.server.port")}$path"))
            .header("Content-Type", "application/json")
            .method(method, HttpRequest.BodyPublishers.ofString(rawBody))
        token?.let { request.header("Authorization", "Bearer $it") }
        headers.forEach { (name, value) -> request.header(name, value) }
        val response = rawHttp.send(request.build(), HttpResponse.BodyHandlers.ofString())
        return Response(response.statusCode(), response.body())
    }

    protected fun uploadPng(token: String, salt: Int): Response {
        val boundary = "----preservation${UUID.randomUUID()}"
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(64) { (it + salt).toByte() }
        val head = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a$salt.png\"\r\nContent-Type: image/png\r\n\r\n"
        val tail = "\r\n--$boundary--\r\n"
        val request = HttpRequest.newBuilder(URI.create("http://localhost:${environment.getProperty("local.server.port")}/api/media"))
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .header("Authorization", "Bearer $token")
            .POST(HttpRequest.BodyPublishers.ofByteArray(head.toByteArray() + png + tail.toByteArray()))
            .build()
        val response = rawHttp.send(request, HttpResponse.BodyHandlers.ofString())
        return Response(response.statusCode(), response.body())
    }

    protected fun fundPartner(partner: Session, credits: Long = 10_000) {
        val wallet = UUID.randomUUID()
        jdbc.update("INSERT INTO ledger_wallets (id, partner_id, status, created_at, updated_at) VALUES (?, ?, 'ACTIVE', now(), now())", wallet, partner.ownerId)
        jdbc.update(
            "INSERT INTO ledger_accounts (id, wallet_id, code, debit_priority, status, balance, created_at, updated_at) VALUES (?, ?, 'MAIN', 1, 'OPEN', ?, now(), now())",
            UUID.randomUUID(), wallet, credits,
        )
    }

    protected fun mainBalance(partner: Session): Long = jdbc.queryForObject(
        """
        SELECT COALESCE(SUM(a.balance), 0) FROM ledger_accounts a
        JOIN ledger_wallets w ON w.id = a.wallet_id WHERE w.partner_id = ? AND a.code = 'MAIN'
        """.trimIndent(),
        Long::class.java, partner.ownerId,
    )!!

    protected fun mediaAsset(ownerId: UUID): UUID {
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO media_assets (id, owner_id, storage_key, content_type, size_bytes, created_at) VALUES (?, ?, ?, 'image/png', 1000, now())", id, ownerId, "test/$id.png")
        return id
    }

    protected fun notificationCount(ownerId: UUID, title: String): Long =
        jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE owner_id = ? AND title LIKE ?", Long::class.java, ownerId, "$title%")!!

    protected fun setNeighborhood(ownerId: UUID, neighborhood: String) {
        jdbc.update("UPDATE owners SET neighborhood = ? WHERE id = ?", neighborhood, ownerId)
    }

    protected fun setLocation(ownerId: UUID, lat: Double, lng: Double) {
        jdbc.update("UPDATE owners SET location = ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography WHERE id = ?", lng, lat, ownerId)
    }

    protected fun recordActivity(
        session: Session,
        petId: UUID,
        startedAt: Instant = Instant.now().minusSeconds(600),
        durationSeconds: Long = 600,
        distanceMeters: Double? = 1000.0,
        share: Boolean = false,
        notes: String? = null,
    ): Response = call("POST", "/api/activities", session.token, mapOf(
        "petId" to petId, "type" to "walk", "startedAt" to startedAt.toString(), "durationSeconds" to durationSeconds,
        "distanceMeters" to distanceMeters, "notes" to notes, "latitude" to -23.56, "longitude" to -46.65, "shareToPawCity" to share,
    ))
}
