package com.pawmotionai.pet.domain.model

import java.time.Instant
import java.time.LocalDate
import java.util.UUID

enum class PetSize {
    SMALL,
    MEDIUM,
    LARGE
}

enum class PetSpecies {
    DOG,
    CAT,
    BIRD,
    RABBIT,
    OTHER
}

enum class PetStatus {
    ACTIVE,
    INACTIVE
}

class Pet private constructor(
    val id: UUID,
    val ownerId: UUID,
    name: String,
    species: PetSpecies,
    breed: String?,
    birthDate: LocalDate?,
    size: PetSize,
    activityPreference: String?,
    val status: PetStatus,
    val createdAt: Instant,
) {

    var name: String = name
        private set
    var species: PetSpecies = species
        private set
    var breed: String? = breed
        private set
    var birthDate: LocalDate? = birthDate
        private set
    var size: PetSize = size
        private set
    var activityPreference: String? = activityPreference
        private set

    val isActive: Boolean get() = status == PetStatus.ACTIVE

    fun updateDetails(name: String?, species: PetSpecies, breed: String?, birthDate: LocalDate?,
                      size: PetSize, activityPreference: String?) {
        require(!name.isNullOrBlank()) { "Pet name cannot be blank" }
        this.name = name.trim()
        this.species = species
        this.breed = trimToNull(breed)
        this.birthDate = birthDate
        this.size = size
        this.activityPreference = trimToNull(activityPreference)
    }

    companion object {
        fun create(id: UUID, ownerId: UUID, name: String, species: PetSpecies, breed: String?, birthDate: LocalDate?,
                   size: PetSize, activityPreference: String?, createdAt: Instant): Pet =
            Pet(id, ownerId, name, species, breed, birthDate, size, activityPreference, PetStatus.ACTIVE, createdAt)

        fun reconstitute(id: UUID, ownerId: UUID, name: String, species: PetSpecies, breed: String?, birthDate: LocalDate?,
                         size: PetSize, activityPreference: String?, status: PetStatus, createdAt: Instant): Pet =
            Pet(id, ownerId, name, species, breed, birthDate, size, activityPreference, status, createdAt)

        private fun trimToNull(value: String?): String? = value?.trim()?.ifEmpty { null }
    }
}
