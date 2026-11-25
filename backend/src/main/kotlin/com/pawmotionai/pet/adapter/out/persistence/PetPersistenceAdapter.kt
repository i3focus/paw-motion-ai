package com.pawmotionai.pet.adapter.out.persistence

import com.pawmotionai.pet.application.port.out.PetRepository
import com.pawmotionai.pet.domain.model.Pet
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class PetPersistenceAdapter(private val repository: PetJpaRepository) : PetRepository {

    override fun save(pet: Pet): Pet {
        val now = Instant.now()
        var entity = repository.findById(pet.id).orElse(null)
        if (entity == null) {
            entity = PetJpaEntity(
                pet.id, pet.ownerId, pet.name, pet.species, pet.breed, pet.birthDate,
                pet.size, pet.activityPreference, pet.status, pet.createdAt, now)
        } else {
            entity.updateDetails(
                pet.name, pet.species, pet.breed, pet.birthDate, pet.size,
                pet.activityPreference, now)
        }
        return toDomain(repository.save(entity))
    }

    override fun lockOwner(ownerId: UUID) {
        repository.lockOwner(ownerId)
    }

    override fun countActiveByOwnerId(ownerId: UUID): Long = repository.countActiveByOwnerId(ownerId)

    override fun findById(id: UUID): Pet? = repository.findById(id).map(::toDomain).orElse(null)

    override fun findByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<Pet> =
        repository.findByOwnerIdOrderByCreatedAtDesc(ownerId).map(::toDomain)

    private fun toDomain(entity: PetJpaEntity): Pet =
        Pet.reconstitute(
            entity.id, entity.ownerId, entity.name, entity.species, entity.breed,
            entity.birthDate, entity.size, entity.activityPreference, entity.status, entity.createdAt)
}
