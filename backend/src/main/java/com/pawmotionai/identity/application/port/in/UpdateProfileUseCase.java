package com.pawmotionai.identity.application.port.in;

import java.util.UUID;

import com.pawmotionai.identity.application.dto.OwnerResponseDto;
import com.pawmotionai.identity.application.dto.UpdateProfileCommand;

public interface UpdateProfileUseCase {

    OwnerResponseDto update(UUID accountId, UpdateProfileCommand command);
}
