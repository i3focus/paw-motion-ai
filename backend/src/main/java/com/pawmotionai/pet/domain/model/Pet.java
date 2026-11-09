package com.pawmotionai.pet.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public class Pet {

    private final UUID id;
    private final UUID ownerId;
    private final String name;
    private final PetSpecies species;
    private final String breed;
    private final LocalDate birthDate;
    private final PetSize size;
    private final String activityPreference;
    private final Instant createdAt;
    private PetStatus status;

    private Pet(UUID id, UUID ownerId, String name, PetSpecies species, String breed, LocalDate birthDate,
                PetSize size, String activityPreference, PetStatus status, Instant createdAt) {
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
    }

    public static Pet create(UUID id, UUID ownerId, String name, PetSpecies species, String breed, LocalDate birthDate,
                             PetSize size, String activityPreference, Instant createdAt) {
        return new Pet(id, ownerId, name, species, breed, birthDate, size, activityPreference, PetStatus.ACTIVE, createdAt);
    }

    public static Pet reconstitute(UUID id, UUID ownerId, String name, PetSpecies species, String breed, LocalDate birthDate,
                                  PetSize size, String activityPreference, PetStatus status, Instant createdAt) {
        return new Pet(id, ownerId, name, species, breed, birthDate, size, activityPreference, status, createdAt);
    }

    public boolean isActive() {
        return status == PetStatus.ACTIVE;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public String getName() {
        return name;
    }

    public PetSpecies getSpecies() {
        return species;
    }

    public String getBreed() {
        return breed;
    }

    public LocalDate getBirthDate() {
        return birthDate;
    }

    public PetSize getSize() {
        return size;
    }

    public String getActivityPreference() {
        return activityPreference;
    }

    public PetStatus getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
