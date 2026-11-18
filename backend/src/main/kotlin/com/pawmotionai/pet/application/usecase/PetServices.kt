package com.pawmotionai.pet.application.usecase

import com.pawmotionai.pet.application.dto.CreatePetCommand
import com.pawmotionai.pet.application.dto.PetResponseDto
import com.pawmotionai.pet.application.dto.UpdatePetCommand
import com.pawmotionai.pet.application.port.`in`.CreatePetUseCase
import com.pawmotionai.pet.application.port.`in`.GetPetUseCase
import com.pawmotionai.pet.application.port.`in`.ListPetsUseCase
import com.pawmotionai.pet.application.port.`in`.UpdatePetUseCase
import com.pawmotionai.pet.application.port.out.PetRepository
import com.pawmotionai.pet.domain.exception.InvalidPetException
import com.pawmotionai.pet.domain.exception.PetNotFoundException
import com.pawmotionai.pet.domain.model.Pet
import com.pawmotionai.pet.domain.model.PetSize
import com.pawmotionai.pet.domain.model.PetSpecies
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.util.UUID

internal object PetParser {

    fun parseSpecies(value: String?): PetSpecies =
        value?.let { runCatching { PetSpecies.valueOf(it.trim().uppercase()) }.getOrNull() }
            ?: throw InvalidPetException("Unsupported pet species: $value")

    fun parseSize(value: String?): PetSize =
        value?.let { runCatching { PetSize.valueOf(it.trim().uppercase()) }.getOrNull() }
            ?: throw InvalidPetException("Unsupported pet size: $value")

    fun trimToNull(value: String?): String? = value?.trim()?.ifEmpty { null }
}

@Service
class CreatePetService(private val petRepository: PetRepository) : CreatePetUseCase {

    @Transactional
    override fun createPet(command: CreatePetCommand): PetResponseDto {
        val pet = Pet.create(
            UUID.randomUUID(),
            command.ownerId,
            command.name.trim(),
            PetParser.parseSpecies(command.species),
            PetParser.trimToNull(command.breed),
            command.birthDate,
            PetParser.parseSize(command.size),
            PetParser.trimToNull(command.activityPreference),
            Instant.now())
        return PetResponseDto.from(petRepository.save(pet))
    }
}

@Service
class GetPetService(private val petRepository: PetRepository) : GetPetUseCase {

    @Transactional(readOnly = true)
    override fun getPet(ownerId: UUID, petId: UUID): PetResponseDto =
        petRepository.findById(petId)
            ?.takeIf { it.ownerId == ownerId }
            ?.let(PetResponseDto::from)
            ?: throw PetNotFoundException(petId)
}

@Service
class ListPetsService(private val petRepository: PetRepository) : ListPetsUseCase {

    @Transactional(readOnly = true)
    override fun listPets(ownerId: UUID): List<PetResponseDto> =
        petRepository.findByOwnerIdOrderByCreatedAtDesc(ownerId).map(PetResponseDto::from)
}

@Service
class UpdatePetService(private val petRepository: PetRepository) : UpdatePetUseCase {

    @Transactional
    override fun updatePet(command: UpdatePetCommand): PetResponseDto {
        val pet = petRepository.findById(command.petId)
            ?.takeIf { it.ownerId == command.ownerId }
            ?: throw PetNotFoundException(command.petId)

        pet.updateDetails(
            command.name,
            PetParser.parseSpecies(command.species),
            PetParser.trimToNull(command.breed),
            command.birthDate,
            PetParser.parseSize(command.size),
            PetParser.trimToNull(command.activityPreference))

        return PetResponseDto.from(petRepository.save(pet))
    }
}
