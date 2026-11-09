package com.pawmotionai.pet.application.usecase;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.pawmotionai.pet.application.dto.PetResponseDto;
import com.pawmotionai.pet.application.dto.UpdatePetCommand;
import com.pawmotionai.pet.application.port.out.PetRepository;
import com.pawmotionai.pet.domain.exception.InvalidPetException;
import com.pawmotionai.pet.domain.exception.PetNotFoundException;
import com.pawmotionai.pet.domain.model.Pet;
import com.pawmotionai.pet.domain.model.PetSize;
import com.pawmotionai.pet.domain.model.PetSpecies;
import com.pawmotionai.pet.domain.model.PetStatus;

class UpdatePetServiceTest {

    private final PetRepository petRepository = mock(PetRepository.class);
    private final UpdatePetService service = new UpdatePetService(petRepository);

    @Test
    void updatesOwnedPetDetails() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        Pet pet = Pet.reconstitute(petId, ownerId, "Luna", PetSpecies.DOG, "Retriever", LocalDate.of(2021, 5, 4),
            PetSize.MEDIUM, "Walks", PetStatus.ACTIVE, Instant.now());
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));
        when(petRepository.save(any(Pet.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PetResponseDto response = service.updatePet(new UpdatePetCommand(
            ownerId, petId, "  Luna Belle  ", "dog", "Golden Retriever", LocalDate.of(2021, 6, 1),
            "large", "Daily runs"));

        assertEquals("Luna Belle", response.name());
        assertEquals("dog", response.species());
        assertEquals("Golden Retriever", response.breed());
        assertEquals(LocalDate.of(2021, 6, 1), response.birthDate());
        assertEquals("large", response.size());
        assertEquals("Daily runs", response.activityPreference());
        verify(petRepository).save(pet);
    }

    @Test
    void rejectsPetOwnedByAnotherOwner() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        Pet pet = Pet.create(petId, UUID.randomUUID(), "Luna", PetSpecies.DOG, null, null,
            PetSize.MEDIUM, null, Instant.now());
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        assertThrows(PetNotFoundException.class, () -> service.updatePet(new UpdatePetCommand(
            ownerId, petId, "Luna", "dog", null, null, "medium", null)));
    }

    @Test
    void rejectsUnsupportedSpecies() {
        UUID ownerId = UUID.randomUUID();
        UUID petId = UUID.randomUUID();
        Pet pet = Pet.create(petId, ownerId, "Luna", PetSpecies.DOG, null, null,
            PetSize.MEDIUM, null, Instant.now());
        when(petRepository.findById(petId)).thenReturn(Optional.of(pet));

        assertThrows(InvalidPetException.class, () -> service.updatePet(new UpdatePetCommand(
            ownerId, petId, "Luna", "dragon", null, null, "medium", null)));
    }
}
