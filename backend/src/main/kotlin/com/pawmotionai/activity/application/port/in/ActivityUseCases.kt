package com.pawmotionai.activity.application.port.`in`

import com.pawmotionai.activity.application.dto.ActivityResponseDto
import com.pawmotionai.activity.application.dto.DashboardSummaryDto
import com.pawmotionai.activity.application.dto.RecordActivityCommand
import java.time.Instant
import java.util.UUID

interface GetDashboardSummaryUseCase {
    fun getSummary(ownerId: UUID): DashboardSummaryDto
}

interface ListActivitiesUseCase {
    fun listActivities(ownerId: UUID, petId: UUID?, from: Instant?, to: Instant?, limit: Int? = null): List<ActivityResponseDto>
}

interface RecordActivityUseCase {
    fun recordActivity(command: RecordActivityCommand): ActivityResponseDto
}
