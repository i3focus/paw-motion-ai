package com.pawmotionai.pet.application.port.`in`

import com.pawmotionai.pet.application.dto.CreatePetCommand
import com.pawmotionai.pet.application.dto.PetResponseDto
import com.pawmotionai.pet.application.dto.UpdatePetCommand
import java.util.UUID

interface CreatePetUseCase {
    fun createPet(command: CreatePetCommand): PetResponseDto
}

interface GetPetUseCase {
    fun getPet(ownerId: UUID, petId: UUID): PetResponseDto
}

interface ListPetsUseCase {
    fun listPets(ownerId: UUID): List<PetResponseDto>
}

interface UpdatePetUseCase {
    fun updatePet(command: UpdatePetCommand): PetResponseDto
}
