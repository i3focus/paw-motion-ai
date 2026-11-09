package com.pawmotionai.activity.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pawmotionai.activity.domain.model.Activity;

public interface ActivityRepository {

    Activity save(Activity activity);

    Optional<Activity> findById(UUID id);

    List<Activity> findByOwnerIdOrderByStartedAtDesc(UUID ownerId);
}
