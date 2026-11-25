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
    fun weeklyTotals(ownerId: UUID, since: Instant): WeeklyTotals
    fun activeDays(ownerId: UUID): Set<LocalDate>
}

data class WeeklyTotals(val count: Int, val distanceMeters: Double, val durationSeconds: Long)
