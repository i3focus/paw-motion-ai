package com.pawmotionai.notification

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant
import java.util.UUID

/** Outbound push gateway (Expo push API). The HTTP adapter is only active when `app.push.expo.enabled=true`. */
interface PushGatewayPort {
    fun send(tokens: List<String>, title: String, body: String, data: Map<String, Any?>)
}

@Component
class LoggingPushGateway : PushGatewayPort {
    private val log = LoggerFactory.getLogger(LoggingPushGateway::class.java)
    override fun send(tokens: List<String>, title: String, body: String, data: Map<String, Any?>) {
        log.info("push stub tokens={}", tokens.size)
    }
}

@Component
class ExpoPushGateway(
    private val mapper: ObjectMapper,
    @Value("\${app.push.expo.enabled:false}") private val enabled: Boolean,
    @Value("\${app.push.expo.url:https://exp.host/--/api/v2/push/send}") private val url: String,
    @Value("\${app.push.expo.connect-timeout-ms:3000}") connectTimeoutMs: Long,
    @Value("\${app.push.expo.request-timeout-ms:8000}") private val requestTimeoutMs: Long,
    @Value("\${app.push.expo.max-attempts:2}") private val maxAttempts: Int,
) : PushGatewayPort {
    private val log = LoggerFactory.getLogger(ExpoPushGateway::class.java)
    private val http: HttpClient = HttpClient.newBuilder().connectTimeout(Duration.ofMillis(connectTimeoutMs)).build()

    override fun send(tokens: List<String>, title: String, body: String, data: Map<String, Any?>) {
        if (!enabled || tokens.isEmpty()) return
        val payload = mapper.writeValueAsString(tokens.map { mapOf("to" to it, "title" to title, "body" to body, "data" to data, "sound" to "default") })
        // Bounded retries (N15): only transport errors, 429 and 5xx are retried; each attempt has hard timeouts.
        for (attempt in 1..maxAttempts.coerceIn(1, 3)) {
            try {
                val request = HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofMillis(requestTimeoutMs))
                    .header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(payload)).build()
                val response = http.send(request, HttpResponse.BodyHandlers.ofString())
                if (response.statusCode() == 200) return
                log.warn("expo push failed status={} attempt={} ", response.statusCode(), attempt)
                if (response.statusCode() != 429 && response.statusCode() < 500) return
            } catch (e: InterruptedException) {
                Thread.currentThread().interrupt()
                return
            } catch (e: Exception) {
                log.warn("expo push error attempt={}: {}", attempt, e.message)
            }
            if (attempt < maxAttempts.coerceIn(1, 3)) Thread.sleep(RETRY_BACKOFF_MS * attempt)
        }
    }

    private companion object {
        const val RETRY_BACKOFF_MS = 250L
    }
}
/** Chooses the real Expo gateway when enabled, otherwise the logging stub (D-014). */
@Component
@org.springframework.context.annotation.Primary
class PushGateway(expo: ExpoPushGateway, stub: LoggingPushGateway, @Value("\${app.push.expo.enabled:false}") enabled: Boolean) : PushGatewayPort {
    private val delegate = if (enabled) expo else stub
    override fun send(tokens: List<String>, title: String, body: String, data: Map<String, Any?>) = delegate.send(tokens, title, body, data)
}

/** Records an in-app notification and fans out to Expo push, honoring per-owner preferences. */
@Service
class Notifier(
    private val jdbc: JdbcClient,
    private val push: PushGatewayPort,
    private val mapper: ObjectMapper,
    private val dispatcher: PostCommitDispatcher,
    private val meterRegistry: MeterRegistry,
) {

    private val log = LoggerFactory.getLogger(Notifier::class.java)

    @Transactional(propagation = Propagation.REQUIRED)
    fun notify(ownerId: UUID, type: NotificationType, title: String, body: String, data: Map<String, Any?> = emptyMap()): UUID {
        val id = UUID.randomUUID()
        jdbc.sql(
            "INSERT INTO notifications (id, owner_id, type, title, body, data, created_at) VALUES (:id, :owner, :type, :title, :body, CAST(:data AS jsonb), now())",
        ).param("id", id).param("owner", ownerId).param("type", type.name).param("title", title.take(120))
            .param("body", body.take(300)).param("data", mapper.writeValueAsString(data)).update()
        meterRegistry.counter("notifications.inserted", "type", type.name).increment()
        if (pushEnabled(ownerId, type)) {
            val tokens = jdbc.sql("SELECT token FROM push_tokens WHERE owner_id = :owner").param("owner", ownerId).query(String::class.java).list().filterNotNull()
            if (tokens.isNotEmpty()) {
                // N15: the provider call runs only after the DB transaction commits, off the request thread.
                dispatcher.afterCommit {
                    runCatching { push.send(tokens, title, body, data + mapOf("notificationId" to id.toString())) }
                        .onSuccess { jdbc.sql("UPDATE notifications SET sent_at = now() WHERE id = :id").param("id", id).update() }
                        .onFailure { jdbc.sql("UPDATE notifications SET error = :err WHERE id = :id").param("err", it.message?.take(300)).param("id", id).update() }
                }
            }
        }
        return id
    }

    private fun pushEnabled(ownerId: UUID, type: NotificationType): Boolean {
        val column = when (type) {
            NotificationType.HEALTH_REMINDER -> "health_reminders"
            NotificationType.PLAYDATE -> "playdates"
            NotificationType.LOST_PET -> "lost_pets"
            NotificationType.BOOKING -> "bookings"
            NotificationType.ACHIEVEMENT, NotificationType.CHALLENGE, NotificationType.SOCIAL, NotificationType.SYSTEM -> "social"
        }
        return jdbc.sql(
            "SELECT COALESCE((SELECT $column FROM notification_preferences WHERE owner_id = :owner), TRUE)",
        ).param("owner", ownerId).query(Boolean::class.java).single()
    }

    /** Fans a geolocated alert out to owners near a point (excluding [excludeOwner]), honoring lost_pets prefs. */
    @Transactional(propagation = Propagation.REQUIRED)
    fun notifyNearby(excludeOwner: UUID, lat: Double, lng: Double, radiusMeters: Int, type: NotificationType, title: String, body: String, data: Map<String, Any?> = emptyMap()) {
        val owners = jdbc.sql(
            """
            SELECT id FROM owners
            WHERE status = 'ACTIVE' AND id <> :exclude AND role = 'OWNER' AND location IS NOT NULL
              AND ST_DWithin(location, ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography, :radius)
            LIMIT 200
            """,
        ).param("exclude", excludeOwner).param("lat", lat).param("lng", lng).param("radius", radiusMeters.coerceIn(500, 50_000))
            .query(UUID::class.java).list().filterNotNull()
        log.info("nearby fanout type={} recipients={}", type, owners.size)
        owners.forEach { notify(it, type, title, body, data) }
    }
}
