package com.pawmotionai.pet.application.port.out;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.pawmotionai.pet.domain.model.Pet;

public interface PetRepository {

    Pet save(Pet pet);

    Optional<Pet> findById(UUID id);

    List<Pet> findByOwnerIdOrderByCreatedAtDesc(UUID ownerId);
}
