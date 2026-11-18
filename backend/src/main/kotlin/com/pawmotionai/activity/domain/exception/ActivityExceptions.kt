package com.pawmotionai.activity.domain.exception

import java.util.UUID

class ActivityNotFoundException(activityId: UUID) : RuntimeException("Activity not found: $activityId")

class InvalidActivityException(message: String) : RuntimeException(message)
