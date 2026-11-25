package com.pawmotionai.activity.domain.exception

import java.util.UUID

class ActivityNotFoundException(activityId: UUID) : RuntimeException("Activity not found: $activityId")

class InvalidActivityException(message: String) : RuntimeException(message)

/** Value outside the plausible range (C3, C21). Mapped to HTTP 400. */
class ActivityOutOfRangeException(message: String) : RuntimeException(message)
