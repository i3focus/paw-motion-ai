package com.pawmotionai.pet.application.usecase;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.port.in.GetPetUseCase;
import com.pawmotionai.pet.application.port.out.PetRepository;
import com.pawmotionai.pet.domain.exception.PetNotFoundException;

@Service
public class GetPetService implements GetPetUseCase {

    private final PetRepository petRepository;

    public GetPetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PetResponseDto getPet(UUID ownerId, UUID petId) {
        return petRepository.findById(petId)
            .filter(pet -> pet.getOwnerId().equals(ownerId))
            .map(PetResponseDto::from)
            .orElseThrow(() -> new PetNotFoundException(petId));
    }
}
