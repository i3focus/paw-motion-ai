package com.pawmotionai.identity.adapter.out.persistence;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException;
import com.pawmotionai.identity.domain.model.Owner;

class OwnerPersistenceAdapterTest {

    @Test
    void mapsConcurrentUniqueViolationToOwnerAlreadyExists() {
        OwnerJpaRepository jpaRepository = mock(OwnerJpaRepository.class);
        OwnerPersistenceAdapter adapter = new OwnerPersistenceAdapter(jpaRepository);
        Owner owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now());

        when(jpaRepository.findById(owner.getId())).thenReturn(Optional.empty());
        when(jpaRepository.saveAndFlush(any(OwnerJpaEntity.class)))
            .thenThrow(new DataIntegrityViolationException("duplicate key"));

        assertThrows(OwnerAlreadyExistsException.class, () -> adapter.save(owner));
    }
}
