package com.pawmotionai.pet.application.port.in;

import java.util.UUID;

import com.pawmotionai.pet.application.dto.PetResponseDto;

public interface GetPetUseCase {

    PetResponseDto getPet(UUID ownerId, UUID petId);
}
