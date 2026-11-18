package com.pawmotionai.pet.application.dto

import com.pawmotionai.pet.domain.model.Pet
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

data class CreatePetCommand(
    val ownerId: UUID,
    val name: String,
    val species: String,
    val breed: String?,
    val birthDate: LocalDate?,
    val size: String,
    val activityPreference: String?,
)

data class UpdatePetCommand(
    val ownerId: UUID,
    val petId: UUID,
    val name: String,
    val species: String,
    val breed: String?,
    val birthDate: LocalDate?,
    val size: String,
    val activityPreference: String?,
)

data class PetResponseDto(
    val id: UUID,
    val ownerId: UUID,
    val name: String,
    val species: String,
    val breed: String?,
    val birthDate: LocalDate?,
    val size: String,
    val activityPreference: String?,
    val status: String,
    val createdAt: Instant,
) {

    companion object {
        fun from(pet: Pet): PetResponseDto =
            PetResponseDto(
                pet.id, pet.ownerId, pet.name, pet.species.name.lowercase(), pet.breed,
                pet.birthDate, pet.size.name.lowercase(), pet.activityPreference,
                pet.status.name.lowercase(), pet.createdAt)
    }
}
