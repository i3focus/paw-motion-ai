package com.pawmotionai.pet.domain.model;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Objects;
import java.util.UUID;

public class Pet {

    private final UUID id;
    private final UUID ownerId;
    private String name;
    private PetSpecies species;
    private String breed;
    private LocalDate birthDate;
    private PetSize size;
    private String activityPreference;
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

    public void updateDetails(String name, PetSpecies species, String breed, LocalDate birthDate,
                              PetSize size, String activityPreference) {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("Pet name cannot be blank");
        }
        this.name = name.trim();
        this.species = Objects.requireNonNull(species, "species");
        this.breed = trimToNull(breed);
        this.birthDate = birthDate;
        this.size = Objects.requireNonNull(size, "size");
        this.activityPreference = trimToNull(activityPreference);
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

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
