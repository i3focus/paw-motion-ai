package com.pawmotionai.pet.adapter.out.persistence;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.pawmotionai.pet.domain.model.PetSize;
import com.pawmotionai.pet.domain.model.PetSpecies;
import com.pawmotionai.pet.domain.model.PetStatus;

@Entity
@Table(name = "pets")
public class PetJpaEntity {

    @Id
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetSpecies species;

    private String breed;

    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetSize size;

    @Column(name = "activity_preference")
    private String activityPreference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PetStatus status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected PetJpaEntity() {
    }

    public PetJpaEntity(UUID id, UUID ownerId, String name, PetSpecies species, String breed, LocalDate birthDate,
                        PetSize size, String activityPreference, PetStatus status, Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.size = size;
        this.activityPreference = activityPreference;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateDetails(String name, PetSpecies species, String breed, LocalDate birthDate,
                              PetSize size, String activityPreference, Instant updatedAt) {
        this.name = name;
        this.species = species;
        this.breed = breed;
        this.birthDate = birthDate;
        this.size = size;
        this.activityPreference = activityPreference;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getName() { return name; }
    public PetSpecies getSpecies() { return species; }
    public String getBreed() { return breed; }
    public LocalDate getBirthDate() { return birthDate; }
    public PetSize getSize() { return size; }
    public String getActivityPreference() { return activityPreference; }
    public PetStatus getStatus() { return status; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public Long getVersion() { return version; }
}
