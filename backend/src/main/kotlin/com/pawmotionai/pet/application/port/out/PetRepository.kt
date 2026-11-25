package com.pawmotionai.pet.application.port.out

import com.pawmotionai.pet.domain.model.Pet
import java.util.UUID

interface PetRepository {

    fun save(pet: Pet): Pet

    /** Locks the owner row until the end of the transaction, serializing per-owner cardinality checks. */
    fun lockOwner(ownerId: UUID)
    fun countActiveByOwnerId(ownerId: UUID): Long
    fun findById(id: UUID): Pet?

    fun findByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<Pet>
}
