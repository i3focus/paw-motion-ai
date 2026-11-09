package com.pawmotionai.identity.application.port.in;

import java.util.UUID;

import com.pawmotionai.identity.application.dto.OwnerResponseDto;

public interface GetCurrentOwnerUseCase {

    OwnerResponseDto getCurrentOwner(UUID ownerId);
}
