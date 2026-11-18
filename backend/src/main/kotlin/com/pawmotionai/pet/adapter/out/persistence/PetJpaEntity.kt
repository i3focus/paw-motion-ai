package com.pawmotionai.pet.adapter.out.persistence

import com.pawmotionai.pet.domain.model.PetSize
import com.pawmotionai.pet.domain.model.PetSpecies
import com.pawmotionai.pet.domain.model.PetStatus
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.Version
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@Entity
@Table(name = "pets")
class PetJpaEntity(

    @Id
    var id: UUID,

    @Column(name = "owner_id", nullable = false)
    var ownerId: UUID,

    @Column(nullable = false)
    var name: String,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var species: PetSpecies,

    var breed: String? = null,

    @Column(name = "birth_date")
    var birthDate: LocalDate? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var size: PetSize,

    @Column(name = "activity_preference")
    var activityPreference: String? = null,

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    var status: PetStatus,

    @Column(name = "created_at", nullable = false)
    var createdAt: Instant,

    @Column(name = "updated_at", nullable = false)
    var updatedAt: Instant,

    @Version
    var version: Long? = null,
) {

    fun updateDetails(name: String, species: PetSpecies, breed: String?, birthDate: LocalDate?,
                      size: PetSize, activityPreference: String?, updatedAt: Instant) {
        this.name = name
        this.species = species
        this.breed = breed
        this.birthDate = birthDate
        this.size = size
        this.activityPreference = activityPreference
        this.updatedAt = updatedAt
    }
}
