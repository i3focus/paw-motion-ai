package com.pawmotionai.pet.application.port.in;

import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.dto.UpdatePetCommand;

public interface UpdatePetUseCase {

    PetResponseDto updatePet(UpdatePetCommand command);
}
