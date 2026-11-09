package com.pawmotionai.activity.domain.exception;

import java.util.UUID;

public class ActivityNotFoundException extends RuntimeException {

    public ActivityNotFoundException(UUID activityId) {
        super("Activity not found: " + activityId);
    }
}
