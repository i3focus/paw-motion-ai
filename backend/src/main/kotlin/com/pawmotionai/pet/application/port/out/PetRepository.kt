package com.pawmotionai.pet.application.port.out

import com.pawmotionai.pet.domain.model.Pet
import java.util.UUID

interface PetRepository {

    fun save(pet: Pet): Pet

    fun findById(id: UUID): Pet?

    fun findByOwnerIdOrderByCreatedAtDesc(ownerId: UUID): List<Pet>
}
