package com.pawmotionai.identity.application.port.out;

import java.util.Optional;
import java.util.UUID;

import com.pawmotionai.identity.domain.model.Owner;

public interface OwnerRepository {

    Owner save(Owner owner);

    Optional<Owner> findById(UUID id);

    Optional<Owner> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
