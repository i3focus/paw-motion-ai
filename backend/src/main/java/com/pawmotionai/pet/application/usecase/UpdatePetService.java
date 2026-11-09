package com.pawmotionai.pet.application.usecase;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.dto.UpdatePetCommand;
import com.pawmotionai.pet.application.port.in.UpdatePetUseCase;
import com.pawmotionai.pet.application.port.out.PetRepository;
import com.pawmotionai.pet.domain.exception.PetNotFoundException;
import com.pawmotionai.pet.domain.model.Pet;

@Service
public class UpdatePetService implements UpdatePetUseCase {

    private final PetRepository petRepository;

    public UpdatePetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    @Transactional
    public PetResponseDto updatePet(UpdatePetCommand command) {
        Pet pet = petRepository.findById(command.petId())
            .filter(candidate -> candidate.getOwnerId().equals(command.ownerId()))
            .orElseThrow(() -> new PetNotFoundException(command.petId()));

        pet.updateDetails(
            command.name(),
            PetParser.parseSpecies(command.species()),
            PetParser.trimToNull(command.breed()),
            command.birthDate(),
            PetParser.parseSize(command.size()),
            PetParser.trimToNull(command.activityPreference()));

        return PetResponseDto.from(petRepository.save(pet));
    }
}
