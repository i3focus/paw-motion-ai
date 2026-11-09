package com.pawmotionai.pet.adapter.out.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Repository;

import com.pawmotionai.pet.application.port.out.PetRepository;
import com.pawmotionai.pet.domain.model.Pet;

@Repository
public class PetPersistenceAdapter implements PetRepository {

    private final PetJpaRepository repository;

    public PetPersistenceAdapter(PetJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Pet save(Pet pet) {
        Instant now = Instant.now();
        PetJpaEntity entity = repository.findById(pet.getId()).orElse(null);
        if (entity == null) {
            entity = new PetJpaEntity(
                pet.getId(), pet.getOwnerId(), pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getBirthDate(),
                pet.getSize(), pet.getActivityPreference(), pet.getStatus(), pet.getCreatedAt(), now);
        } else {
            entity.updateDetails(
                pet.getName(), pet.getSpecies(), pet.getBreed(), pet.getBirthDate(), pet.getSize(),
                pet.getActivityPreference(), now);
        }
        return toDomain(repository.save(entity));
    }

    @Override
    public Optional<Pet> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public List<Pet> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId) {
        return repository.findByOwnerIdOrderByCreatedAtDesc(ownerId).stream().map(this::toDomain).toList();
    }

    private Pet toDomain(PetJpaEntity entity) {
        return Pet.reconstitute(
            entity.getId(), entity.getOwnerId(), entity.getName(), entity.getSpecies(), entity.getBreed(),
            entity.getBirthDate(), entity.getSize(), entity.getActivityPreference(), entity.getStatus(), entity.getCreatedAt());
    }
}
