package com.pawmotionai.activity.application.port.out

import com.pawmotionai.activity.domain.model.Activity
import java.util.UUID

interface ActivityRepository {

    fun save(activity: Activity): Activity

    fun findById(id: UUID): Activity?

    fun findByOwnerIdOrderByStartedAtDesc(ownerId: UUID): List<Activity>
}
