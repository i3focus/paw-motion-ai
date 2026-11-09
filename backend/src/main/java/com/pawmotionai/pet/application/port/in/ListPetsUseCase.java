package com.pawmotionai.pet.application.port.in;

import java.util.List;
import java.util.UUID;

import com.pawmotionai.pet.application.dto.PetResponseDto;

public interface ListPetsUseCase {

    List<PetResponseDto> listPets(UUID ownerId);
}
