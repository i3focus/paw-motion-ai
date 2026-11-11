package com.pawmotionai.shared

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.util.UUID

@Component
class PetAccess(private val jdbc: JdbcClient) {

    fun requireOwnedActivePet(ownerId: UUID, petId: UUID) {
        val owner = jdbc.sql("SELECT owner_id FROM pets WHERE id = :id AND status = 'ACTIVE'").param("id", petId)
            .query(UUID::class.java).optional().orElseThrow { NotFoundException("Pet not found") }
        if (owner != ownerId) throw ForbiddenException("Pet belongs to another owner")
    }

    fun requireActivePets(petIds: Collection<UUID>) {
        if (petIds.isEmpty()) return
        val found = jdbc.sql("SELECT COUNT(*) FROM pets WHERE id IN (:ids) AND status = 'ACTIVE'").param("ids", petIds.toSet())
            .query(Long::class.java).single()
        if (found != petIds.toSet().size.toLong()) throw NotFoundException("Tagged pet not found")
    }
}
