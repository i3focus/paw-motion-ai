package com.pawmotionai.pet.application.usecase

import com.pawmotionai.pet.application.dto.UpdatePetCommand
import com.pawmotionai.pet.application.port.out.PetRepository
import com.pawmotionai.pet.domain.exception.InvalidPetException
import com.pawmotionai.pet.domain.exception.PetNotFoundException
import com.pawmotionai.pet.domain.model.Pet
import com.pawmotionai.pet.domain.model.PetSize
import com.pawmotionai.pet.domain.model.PetSpecies
import com.pawmotionai.pet.domain.model.PetStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

class UpdatePetServiceTest {

    private val petRepository = mock(PetRepository::class.java)
    private val service = UpdatePetService(petRepository)

    @Test
    fun updatesOwnedPetDetails() {
        val ownerId = UUID.randomUUID()
        val petId = UUID.randomUUID()
        val pet = Pet.reconstitute(petId, ownerId, "Luna", PetSpecies.DOG, "Retriever", LocalDate.of(2021, 5, 4),
            PetSize.MEDIUM, "Walks", PetStatus.ACTIVE, Instant.now())
        `when`(petRepository.findById(petId)).thenReturn(pet)
        `when`(petRepository.save(any(Pet::class.java) ?: pet)).thenAnswer { it.getArgument<Pet>(0) }

        val response = service.updatePet(UpdatePetCommand(
            ownerId, petId, "  Luna Belle  ", "dog", "Golden Retriever", LocalDate.of(2021, 6, 1),
            "large", "Daily runs"))

        assertEquals("Luna Belle", response.name)
        assertEquals("dog", response.species)
        assertEquals("Golden Retriever", response.breed)
        assertEquals(LocalDate.of(2021, 6, 1), response.birthDate)
        assertEquals("large", response.size)
        assertEquals("Daily runs", response.activityPreference)
        verify(petRepository).save(pet)
    }

    @Test
    fun rejectsPetOwnedByAnotherOwner() {
        val ownerId = UUID.randomUUID()
        val petId = UUID.randomUUID()
        val pet = Pet.create(petId, UUID.randomUUID(), "Luna", PetSpecies.DOG, null, null,
            PetSize.MEDIUM, null, Instant.now())
        `when`(petRepository.findById(petId)).thenReturn(pet)

        assertThrows(PetNotFoundException::class.java) {
            service.updatePet(UpdatePetCommand(ownerId, petId, "Luna", "dog", null, null, "medium", null))
        }
    }

    @Test
    fun rejectsUnsupportedSpecies() {
        val ownerId = UUID.randomUUID()
        val petId = UUID.randomUUID()
        val pet = Pet.create(petId, ownerId, "Luna", PetSpecies.DOG, null, null,
            PetSize.MEDIUM, null, Instant.now())
        `when`(petRepository.findById(petId)).thenReturn(pet)

        assertThrows(InvalidPetException::class.java) {
            service.updatePet(UpdatePetCommand(ownerId, petId, "Luna", "dragon", null, null, "medium", null))
        }
    }
}
