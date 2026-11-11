package com.pawmotionai.support

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.core.env.Environment
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.oauth2.jose.jws.MacAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.testcontainers.postgresql.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Instant
import java.util.UUID

@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = [
        "app.security.jwt.secret=integration-test-secret-integration-test-secret",
        "app.billing.reconciliation-interval-ms=3600000",
        "app.campaign.expiration-interval-ms=3600000",
    ],
)
abstract class ApiIntegrationTest {

    companion object {
        @JvmField
        @ServiceConnection
        val postgres: PostgreSQLContainer =
            PostgreSQLContainer(DockerImageName.parse("postgis/postgis:17-3.5").asCompatibleSubstituteFor("postgres"))
                .apply { start() }
    }

    data class Response(val status: Int, val body: String)

    data class Session(val token: String, val ownerId: UUID)

    @Autowired
    protected lateinit var environment: Environment

    @Autowired
    protected lateinit var mapper: ObjectMapper

    @Autowired
    protected lateinit var jdbc: JdbcTemplate

    @Autowired
    private lateinit var jwtEncoder: JwtEncoder

    private val http: HttpClient = HttpClient.newHttpClient()

    protected fun call(method: String, path: String, token: String? = null, body: Any? = null): Response {
        val builder = HttpRequest.newBuilder(URI.create("http://localhost:${environment.getProperty("local.server.port")}$path"))
            .header("Content-Type", "application/json")
        token?.let { builder.header("Authorization", "Bearer $it") }
        val publisher = body?.let { HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(it)) }
            ?: HttpRequest.BodyPublishers.noBody()
        val response = http.send(builder.method(method, publisher).build(), HttpResponse.BodyHandlers.ofString())
        return Response(response.statusCode(), response.body())
    }

    protected fun json(response: Response): JsonNode = mapper.readTree(response.body)

    protected fun JsonNode.elements(): List<JsonNode> = (0 until size()).map { get(it) }

    protected fun expect(status: Int, response: Response): JsonNode {
        check(response.status == status) { "Expected HTTP $status but got ${response.status}: ${response.body}" }
        return if (response.body.isBlank()) mapper.nullNode() else json(response)
    }

    /** Inserts an active account and mints its JWT directly, so suites are not throttled by the auth rate limiter. */
    protected fun registerOwner(name: String = "Owner", role: String = "OWNER"): Session {
        val id = UUID.randomUUID()
        val email = "${name.lowercase()}-$id@example.com"
        jdbc.update(
            """
            INSERT INTO owners (id, email, password_hash, display_name, role, status, created_at, updated_at)
            VALUES (?, ?, 'not-used', ?, ?, 'ACTIVE', now(), now())
            """.trimIndent(),
            id, email, name, role,
        )
        val now = Instant.now()
        val claims = JwtClaimsSet.builder()
            .subject(id.toString())
            .issuedAt(now)
            .expiresAt(now.plusSeconds(3600))
            .claim("email", email)
            .claim("displayName", name)
            .claim("role", role)
            .build()
        val token = jwtEncoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).tokenValue
        return Session(token, id)
    }

    protected fun createPet(session: Session, name: String = "Thor", species: String = "dog"): UUID {
        val body = expect(201, call("POST", "/api/pets", session.token, mapOf("name" to name, "species" to species, "size" to "medium")))
        return UUID.fromString(body["id"].asString())
    }
}
