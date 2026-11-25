package com.pawmotionai.exploratory

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.UUID

/**
 * Marks a bug-condition exploration test (spec `security-audit-remediation`, task 1).
 *
 * These tests encode the EXPECTED behavior and therefore FAIL on the unfixed code. They are excluded from the
 * default Gradle run and enabled with `./gradlew test -Pexploratory` (see docs/security/exploratory-bug-condition-tests.md).
 */
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
@Tag("exploratory")
annotation class Exploratory

/**
 * Provisional numeric limits used only to give the exploratory tests a concrete threshold. The real values are an open
 * product decision (tasks.md 3.2) and will be calibrated in `shadow` mode. Update here when the decision is made.
 */
object ProvisionalLimits {
    const val MAX_PETS_PER_OWNER = 20
    const val MAX_ACTIVE_ALERTS_PER_OWNER = 3
    const val MAX_SIGHTINGS_PER_REPORTER = 3
    const val MAX_MEDIA_OBJECTS_PER_OWNER = 50
    const val DAILY_XP_CAP_PER_PET_AND_SOURCE = 150
    const val MAX_SLUG_ATTEMPTS = 4
    const val MAX_DURATION_SECONDS = 86_400L
    const val MAX_DISTANCE_METERS = 500_000.0
    const val READ_BURST_PER_MINUTE = 1_000
}

/** Shared helpers for exploratory integration tests (Postgres via Testcontainers). */
abstract class ExploratoryApiTest : ApiIntegrationTest() {

    @Autowired
    private lateinit var exploratoryEncoder: JwtEncoder

    private val rawHttp: HttpClient = HttpClient.newHttpClient()

    /** Mints a JWT with arbitrary role claims, independent of the role stored in the database. */
    protected fun mintToken(ownerId: UUID, claimedRoles: List<String>): String {
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(ownerId.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .claim("role", claimedRoles.first())
            .claim("roles", claimedRoles)
            .build()
        return exploratoryEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).tokenValue
    }

    /** Sends [rawBody] as-is (needed for payloads Jackson would refuse to build, such as NaN). */
    protected fun rawJsonCall(method: String, path: String, token: String, rawBody: String, headers: Map<String, String> = emptyMap()): Response {
        val request = HttpRequest.newBuilder(URI.create("http://localhost:${environment.getProperty("local.server.port")}$path"))
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $token")
            .method(method, HttpRequest.BodyPublishers.ofString(rawBody))
        headers.forEach { (name, value) -> request.header(name, value) }
        val built = request.build()
        val response = rawHttp.send(built, HttpResponse.BodyHandlers.ofString())
        return Response(response.statusCode(), response.body())
    }

    protected fun uploadPng(token: String, salt: Int): Response {
        val boundary = "----exploratory${UUID.randomUUID()}"
        val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(64) { (it + salt).toByte() }
        val head = "--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"a$salt.png\"\r\nContent-Type: image/png\r\n\r\n"
        val tail = "\r\n--$boundary--\r\n"
        val body = head.toByteArray() + png + tail.toByteArray()
        val request = HttpRequest.newBuilder(URI.create("http://localhost:${environment.getProperty("local.server.port")}/api/media"))
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .header("Authorization", "Bearer $token")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body))
            .build()
        val response = rawHttp.send(request, HttpResponse.BodyHandlers.ofString())
        return Response(response.statusCode(), response.body())
    }

    protected fun setNeighborhood(ownerId: UUID, neighborhood: String) {
        jdbc.update("UPDATE owners SET neighborhood = ? WHERE id = ?", neighborhood, ownerId)
    }

    protected fun setLocation(ownerId: UUID, lat: Double, lng: Double) {
        jdbc.update("UPDATE owners SET location = ST_SetSRID(ST_MakePoint(?, ?), 4326)::geography WHERE id = ?", lng, lat, ownerId)
    }

    protected fun notificationCount(ownerId: UUID, title: String): Long =
        jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE owner_id = ? AND title LIKE ?", Long::class.java, ownerId, "$title%")!!

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

    protected fun fundPartner(partner: Session, credits: Long = 10_000) {
        val wallet = UUID.randomUUID()
        jdbc.update("INSERT INTO ledger_wallets (id, partner_id, status, created_at, updated_at) VALUES (?, ?, 'ACTIVE', now(), now())", wallet, partner.ownerId)
        jdbc.update(
            "INSERT INTO ledger_accounts (id, wallet_id, code, debit_priority, status, balance, created_at, updated_at) VALUES (?, ?, 'MAIN', 1, 'OPEN', ?, now(), now())",
            UUID.randomUUID(), wallet, credits,
        )
    }

    protected fun mediaAsset(ownerId: UUID): UUID {
        val id = UUID.randomUUID()
        jdbc.update("INSERT INTO media_assets (id, owner_id, storage_key, content_type, size_bytes, created_at) VALUES (?, ?, ?, 'image/png', 1000, now())", id, ownerId, "test/$id.png")
        return id
    }

    // ---- PostgreSQL statement log (used to count SQL without touching production code) ----

    protected fun startStatementLog(): Int {
        jdbc.execute("ALTER SYSTEM SET log_statement = 'all'")
        jdbc.execute("SELECT pg_reload_conf()")
        Thread.sleep(500)
        return postgres.logs.length
    }

    protected fun statementLogSince(mark: Int): List<String> {
        Thread.sleep(800)
        return postgres.logs.substring(mark).lines()
    }

    protected fun stopStatementLog() {
        jdbc.execute("ALTER SYSTEM RESET log_statement")
        jdbc.execute("SELECT pg_reload_conf()")
    }
}
