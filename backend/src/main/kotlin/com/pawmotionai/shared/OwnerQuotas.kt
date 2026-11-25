package com.pawmotionai.shared

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.util.UUID

/**
 * N9 / N2: per-owner creation quotas for communities, community events and activities.
 *
 * Every check first takes a row lock on the owner (`FOR NO KEY UPDATE`), so concurrent creations by the same owner are
 * serialized and the caps hold under concurrency (same technique as the pet and lost-pet caps). The lock is held until
 * the caller's transaction ends, so callers must invoke these inside the transaction that inserts the row.
 *
 * `app.limits.mode`: `enforce` rejects with 409, `shadow` only logs, `off` skips (rollback). Content moderation is out of
 * scope: these quotas only bound volume per owner.
 */
@Component
class OwnerQuotas(
    private val jdbc: JdbcClient,
    @Value("\${app.limits.mode:enforce}") private val mode: String,
    @Value("\${app.limits.max-communities-per-owner:5}") private val maxCommunities: Int,
    @Value("\${app.limits.max-communities-per-day:3}") private val maxCommunitiesPerDay: Int,
    @Value("\${app.limits.max-active-events-per-owner:20}") private val maxActiveEvents: Int,
    @Value("\${app.limits.max-events-per-day:10}") private val maxEventsPerDay: Int,
    @Value("\${app.limits.max-activities-per-day:200}") private val maxActivitiesPerDay: Int,
) {
    private val log = LoggerFactory.getLogger(OwnerQuotas::class.java)

    fun beforeCreateCommunity(ownerId: UUID) {
        if (!lock(ownerId)) return
        val total = count("SELECT COUNT(*) FROM communities WHERE created_by = :o", ownerId)
        val today = count("SELECT COUNT(*) FROM communities WHERE created_by = :o AND created_at > now() - interval '1 day'", ownerId)
        check(ownerId, total >= maxCommunities, "Community limit reached ($maxCommunities per owner)")
        check(ownerId, today >= maxCommunitiesPerDay, "Daily community creation limit reached ($maxCommunitiesPerDay per day)")
    }

    fun beforeCreateEvent(ownerId: UUID) {
        if (!lock(ownerId)) return
        val active = count("SELECT COUNT(*) FROM community_events WHERE created_by = :o AND starts_at > now()", ownerId)
        val today = count("SELECT COUNT(*) FROM community_events WHERE created_by = :o AND created_at > now() - interval '1 day'", ownerId)
        check(ownerId, active >= maxActiveEvents, "Active event limit reached ($maxActiveEvents per owner)")
        check(ownerId, today >= maxEventsPerDay, "Daily event creation limit reached ($maxEventsPerDay per day)")
    }

    fun beforeRecordActivity(ownerId: UUID) {
        if (!lock(ownerId)) return
        val today = count("SELECT COUNT(*) FROM activities WHERE owner_id = :o AND created_at > now() - interval '1 day'", ownerId)
        check(ownerId, today >= maxActivitiesPerDay, "Daily activity limit reached ($maxActivitiesPerDay per day)")
    }

    private fun lock(ownerId: UUID): Boolean {
        if (mode == "off") return false
        jdbc.sql("SELECT id FROM owners WHERE id = :id FOR NO KEY UPDATE").param("id", ownerId).query(UUID::class.java).optional()
        return true
    }

    private fun count(sql: String, ownerId: UUID): Long = jdbc.sql(sql).param("o", ownerId).query(Long::class.java).single()

    private fun check(ownerId: UUID, exceeded: Boolean, message: String) {
        if (!exceeded) return
        if (mode == "enforce") throw ConflictException(message)
        log.warn("owner quota would be exceeded owner={} reason={}", ownerId, message)
    }
}
