package com.pawmotionai.pet.application.port.in;

import com.pawmotionai.pet.application.dto.CreatePetCommand;
import com.pawmotionai.pet.application.dto.PetResponseDto;

public interface CreatePetUseCase {

    PetResponseDto createPet(CreatePetCommand command);
}
