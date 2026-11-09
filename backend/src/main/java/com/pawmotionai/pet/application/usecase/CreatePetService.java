package com.pawmotionai.pet.application.usecase;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.pawmotionai.pet.application.dto.CreatePetCommand;
import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.port.in.CreatePetUseCase;
import com.pawmotionai.pet.application.port.out.PetRepository;
import com.pawmotionai.pet.domain.exception.InvalidPetException;
import com.pawmotionai.pet.domain.model.Pet;
import com.pawmotionai.pet.domain.model.PetSize;
import com.pawmotionai.pet.domain.model.PetSpecies;

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
            parseSpecies(command.species()),
            trimToNull(command.breed()),
            command.birthDate(),
            parseSize(command.size()),
            trimToNull(command.activityPreference()),
            Instant.now());
        return PetResponseDto.from(petRepository.save(pet));
    }

    private PetSpecies parseSpecies(String value) {
        try {
            return PetSpecies.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidPetException("Unsupported pet species: " + value);
        }
    }

    private PetSize parseSize(String value) {
        try {
            return PetSize.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException | NullPointerException ex) {
            throw new InvalidPetException("Unsupported pet size: " + value);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
