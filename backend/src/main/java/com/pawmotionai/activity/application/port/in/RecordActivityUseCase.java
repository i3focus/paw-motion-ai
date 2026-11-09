package com.pawmotionai.activity.application.port.in;

import com.pawmotionai.activity.application.dto.ActivityResponseDto;
import com.pawmotionai.activity.application.dto.RecordActivityCommand;

public interface RecordActivityUseCase {

    ActivityResponseDto recordActivity(RecordActivityCommand command);
}
