package com.pawmotionai.notification

import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.util.UUID

@Service
class NotificationService(private val jdbc: JdbcClient, private val notifier: Notifier, private val mapper: ObjectMapper) {

    private val log = LoggerFactory.getLogger(NotificationService::class.java)

    @Transactional
    fun registerToken(ownerId: UUID, token: String, platform: PushPlatform) {
        jdbc.sql(
            """
            INSERT INTO push_tokens (token, owner_id, platform, created_at, last_seen_at)
            VALUES (:token, :owner, :platform, now(), now())
            ON CONFLICT (token) DO UPDATE SET owner_id = EXCLUDED.owner_id, platform = EXCLUDED.platform, last_seen_at = now()
            """,
        ).param("token", token.trim()).param("owner", ownerId).param("platform", platform.name).update()
    }

    @Transactional
    fun removeToken(ownerId: UUID, token: String) {
        jdbc.sql("DELETE FROM push_tokens WHERE token = :token AND owner_id = :owner").param("token", token).param("owner", ownerId).update()
    }

    @Transactional(readOnly = true)
    fun preferences(ownerId: UUID): NotificationPreferencesDto = jdbc.sql(
        "SELECT health_reminders, social, playdates, lost_pets, bookings FROM notification_preferences WHERE owner_id = :owner",
    ).param("owner", ownerId).query { rs, _ ->
        NotificationPreferencesDto(rs.getBoolean(1), rs.getBoolean(2), rs.getBoolean(3), rs.getBoolean(4), rs.getBoolean(5))
    }.optional().orElse(NotificationPreferencesDto())

    @Transactional
    fun updatePreferences(ownerId: UUID, prefs: NotificationPreferencesDto): NotificationPreferencesDto {
        jdbc.sql(
            """
            INSERT INTO notification_preferences (owner_id, health_reminders, social, playdates, lost_pets, bookings, updated_at)
            VALUES (:owner, :h, :s, :p, :l, :b, now())
            ON CONFLICT (owner_id) DO UPDATE SET health_reminders = :h, social = :s, playdates = :p, lost_pets = :l, bookings = :b, updated_at = now()
            """,
        ).param("owner", ownerId).param("h", prefs.healthReminders).param("s", prefs.social).param("p", prefs.playdates)
            .param("l", prefs.lostPets).param("b", prefs.bookings).update()
        return prefs
    }

    @Transactional(readOnly = true)
    fun notifications(ownerId: UUID, page: PageRequest): PageResponse<NotificationDto> = PageResponse.of(
        jdbc.sql(
            "SELECT id, type, title, body, data, read_at, created_at FROM notifications WHERE owner_id = :owner ORDER BY created_at DESC, id LIMIT :limit OFFSET :offset",
        ).param("owner", ownerId).param("limit", page.limit).param("offset", page.offset).query { rs, _ ->
            NotificationDto(rs.getObject(1, UUID::class.java), NotificationType.valueOf(rs.getString(2)), rs.getString(3), rs.getString(4),
                mapper.readValue(rs.getString(5), Map::class.java) as Map<String, Any?>, rs.getTimestamp(6) != null, rs.getTimestamp(7).toInstant())
        }.list(), page.page, page.size,
    )

    @Transactional(readOnly = true)
    fun unreadCount(ownerId: UUID): Long =
        jdbc.sql("SELECT COUNT(*) FROM notifications WHERE owner_id = :owner AND read_at IS NULL").param("owner", ownerId).query(Long::class.java).single()

    @Transactional
    fun markRead(ownerId: UUID, notificationId: UUID) {
        val updated = jdbc.sql("UPDATE notifications SET read_at = now() WHERE id = :id AND owner_id = :owner AND read_at IS NULL")
            .param("id", notificationId).param("owner", ownerId).update()
        if (updated == 0) {
            jdbc.sql("SELECT 1 FROM notifications WHERE id = :id AND owner_id = :owner").param("id", notificationId).param("owner", ownerId)
                .query(Int::class.java).optional().orElseThrow { NotFoundException("Notification not found") }
        }
    }

    @Transactional
    fun markAllRead(ownerId: UUID) {
        jdbc.sql("UPDATE notifications SET read_at = now() WHERE owner_id = :owner AND read_at IS NULL").param("owner", ownerId).update()
    }

    /** Sends due health reminders (vaccines, deworming, vet visits). Runs every 15 minutes. */
    @Scheduled(fixedDelayString = "\${app.notifications.reminder-interval-ms:900000}", initialDelayString = "\${app.notifications.reminder-initial-delay-ms:60000}")
    @Transactional
    fun dispatchHealthReminders() {
        val due = jdbc.sql(
            """
            SELECT r.id, r.owner_id, r.record_id, hr.name, hr.due_on, p.name AS pet_name
            FROM health_reminders r
            JOIN health_records hr ON hr.id = r.record_id
            JOIN pets p ON p.id = hr.pet_id
            WHERE r.sent_at IS NULL AND r.remind_at <= now()
            ORDER BY r.remind_at LIMIT 100
            """,
        ).query { rs, _ ->
            ReminderRow(rs.getObject("id", UUID::class.java), rs.getObject("owner_id", UUID::class.java), rs.getObject("record_id", UUID::class.java),
                rs.getString("name"), rs.getDate("due_on")?.toLocalDate()?.toString(), rs.getString("pet_name"))
        }.list()
        due.forEach { r ->
            notifier.notify(r.ownerId, NotificationType.HEALTH_REMINDER, "Health reminder for ${r.petName}",
                "${r.name}${r.dueOn?.let { " is due on $it" } ?: " is due soon"}.", mapOf("recordId" to r.recordId.toString()))
            jdbc.sql("UPDATE health_reminders SET sent_at = now() WHERE id = :id").param("id", r.id).update()
        }
        if (due.isNotEmpty()) log.info("health reminders dispatched count={}", due.size)
    }

    private data class ReminderRow(val id: UUID, val ownerId: UUID, val recordId: UUID, val name: String, val dueOn: String?, val petName: String)
}
