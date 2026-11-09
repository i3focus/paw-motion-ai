package com.pawmotionai.pet.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.pet.application.dto.CreatePetCommand;
import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.port.in.CreatePetUseCase;
import com.pawmotionai.pet.application.port.out.PetRepository;
import com.pawmotionai.pet.domain.model.Pet;

@Service
public class CreatePetService implements CreatePetUseCase {

    private final PetRepository petRepository;

    public CreatePetService(PetRepository petRepository) {
        this.petRepository = petRepository;
    }

    @Override
    @Transactional
    public PetResponseDto createPet(CreatePetCommand command) {
        Pet pet = Pet.create(
            UUID.randomUUID(),
            command.ownerId(),
            command.name().trim(),
            PetParser.parseSpecies(command.species()),
            PetParser.trimToNull(command.breed()),
            command.birthDate(),
            PetParser.parseSize(command.size()),
            PetParser.trimToNull(command.activityPreference()),
            Instant.now());
        return PetResponseDto.from(petRepository.save(pet));
    }
}
