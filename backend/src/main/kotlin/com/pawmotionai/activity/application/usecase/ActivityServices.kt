package com.pawmotionai.activity.application.usecase

import com.pawmotionai.activity.application.dto.ActivityResponseDto
import com.pawmotionai.activity.application.dto.DashboardSummaryDto
import com.pawmotionai.activity.application.dto.RecordActivityCommand
import com.pawmotionai.activity.application.port.`in`.GetDashboardSummaryUseCase
import com.pawmotionai.activity.application.port.`in`.ListActivitiesUseCase
import com.pawmotionai.activity.application.port.`in`.RecordActivityUseCase
import com.pawmotionai.activity.application.port.out.ActivityRepository
import com.pawmotionai.activity.domain.exception.InvalidActivityException
import com.pawmotionai.activity.domain.model.Activity
import com.pawmotionai.activity.domain.model.ActivityType
import com.pawmotionai.activity.domain.service.ActivityStatisticsCalculator
import com.pawmotionai.pet.application.port.`in`.GetPetUseCase
import com.pawmotionai.pet.application.port.`in`.ListPetsUseCase
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID

@Service
class RecordActivityService(
    private val activityRepository: ActivityRepository,
    private val getPetUseCase: GetPetUseCase,
) : RecordActivityUseCase {

    @Transactional
    override fun recordActivity(command: RecordActivityCommand): ActivityResponseDto {
        val pet = getPetUseCase.getPet(command.ownerId, command.petId)
        if (pet.status != "active") {
            throw InvalidActivityException("Cannot record activity for an inactive pet")
        }

        val activity = Activity.record(
            UUID.randomUUID(),
            command.ownerId,
            command.petId,
            parseType(command.type),
            command.startedAt,
            command.durationSeconds,
            command.distanceMeters,
            trimToNull(command.notes),
            Instant.now(),
            command.latitude,
            command.longitude,
            command.shareToPawCity == true)
        return ActivityResponseDto.from(activityRepository.save(activity))
    }

    private fun parseType(value: String): ActivityType = try {
        ActivityType.valueOf(value.trim().uppercase().replace('-', '_'))
    } catch (exception: RuntimeException) {
        throw InvalidActivityException("Unsupported activity type: $value")
    }

    private fun trimToNull(value: String?): String? = value?.trim()?.ifEmpty { null }
}

@Service
class ListActivitiesService(private val activityRepository: ActivityRepository) : ListActivitiesUseCase {

    @Transactional(readOnly = true)
    override fun listActivities(ownerId: UUID, petId: UUID?, from: Instant?, to: Instant?): List<ActivityResponseDto> =
        activityRepository.findByOwnerIdOrderByStartedAtDesc(ownerId)
            .filter { petId == null || it.petId == petId }
            .filter { from == null || !it.startedAt.isBefore(from) }
            .filter { to == null || !it.startedAt.isAfter(to) }
            .map(ActivityResponseDto::from)
}

@Service
class GetDashboardSummaryService(
    private val activityRepository: ActivityRepository,
    private val listPetsUseCase: ListPetsUseCase,
) : GetDashboardSummaryUseCase {

    @Transactional(readOnly = true)
    override fun getSummary(ownerId: UUID): DashboardSummaryDto {
        val now = Instant.now()
        val weekStart = now.minus(7, ChronoUnit.DAYS)
        val activities = activityRepository.findByOwnerIdOrderByStartedAtDesc(ownerId)
        val last7Days = activities.filter { !it.startedAt.isBefore(weekStart) }

        val distance = last7Days.sumOf { it.distanceMeters ?: 0.0 }
        val duration = last7Days.sumOf { it.durationSeconds }
        val streak = ActivityStatisticsCalculator.currentStreakDays(activities, now)
        val recent = activities.take(RECENT_ACTIVITY_LIMIT).map(ActivityResponseDto::from)

        return DashboardSummaryDto(
            ownerId,
            listPetsUseCase.listPets(ownerId).size,
            last7Days.size,
            distance,
            duration,
            streak,
            recent)
    }

    private companion object {
        const val RECENT_ACTIVITY_LIMIT = 5
    }
}
