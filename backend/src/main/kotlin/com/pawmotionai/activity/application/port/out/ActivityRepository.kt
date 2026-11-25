package com.pawmotionai.activity.application.port.out

import com.pawmotionai.activity.domain.model.Activity
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

interface ActivityRepository {

    fun save(activity: Activity): Activity

    fun findById(id: UUID): Activity?

    fun findByOwnerIdOrderByStartedAtDesc(ownerId: UUID): List<Activity>
    fun findFiltered(ownerId: UUID, petId: UUID?, from: Instant?, to: Instant?, limit: Int): List<Activity>
    /** Aggregates (count, sum distance, sum duration) over the newest [limit] activities since [from], computed in SQL. */
    fun findForInsights(ownerId: UUID, petId: UUID?, from: Instant, limit: Int): WeeklyTotals
    fun weeklyTotals(ownerId: UUID, since: Instant): WeeklyTotals
    fun activeDays(ownerId: UUID): Set<LocalDate>
}

data class WeeklyTotals(val count: Int, val distanceMeters: Double, val durationSeconds: Long)
