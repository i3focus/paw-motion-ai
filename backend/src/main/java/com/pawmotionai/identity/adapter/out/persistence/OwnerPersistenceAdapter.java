package com.pawmotionai.identity.adapter.out.persistence;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

import com.pawmotionai.identity.application.port.out.OwnerRepository;
import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException;
import com.pawmotionai.identity.domain.model.Owner;
import com.pawmotionai.identity.domain.model.OwnerStatus;

@Repository
public class OwnerPersistenceAdapter implements OwnerRepository {

    private final OwnerJpaRepository repository;

    public OwnerPersistenceAdapter(OwnerJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Owner save(Owner owner) {
        OwnerJpaEntity entity = repository.findById(owner.getId())
            .orElseGet(() -> new OwnerJpaEntity(
                owner.getId(), owner.getEmail(), owner.getPasswordHash(), owner.getDisplayName(), owner.getRole(),
                owner.getStatus(), owner.getCreatedAt(), Instant.now(), null, null, null, null, null, null));
        entity.updateProfile(owner.getDisplayName(), owner.getBio(), owner.getPhone(), owner.getAvatarUrl(),
            owner.getBusinessName(), owner.getWebsite(), owner.getPartnerDescription());
        try {
            return toDomain(repository.saveAndFlush(entity));
        } catch (DataIntegrityViolationException exception) {
            if (isEmailUniqueViolation(exception)) {
                throw new OwnerAlreadyExistsException(owner.getEmail());
            }
            throw exception;
        }
    }

    @Override
    public Optional<Owner> findById(UUID id) {
        return repository.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Owner> findByEmailIgnoreCase(String email) {
        return repository.findByEmailIgnoreCase(email).map(this::toDomain);
    }

    @Override
    public boolean existsByEmailIgnoreCase(String email) {
        return repository.existsByEmailIgnoreCase(email);
    }

    private boolean isEmailUniqueViolation(DataIntegrityViolationException exception) {
        String message = exception.getMessage();
        return message != null && (message.contains("uq_owners_email") || message.contains("duplicate key value violates unique constraint"));
    }

    private Owner toDomain(OwnerJpaEntity entity) {
        return Owner.reconstitute(
            entity.getId(), entity.getEmail(), entity.getPasswordHash(), entity.getDisplayName(), entity.getRole(),
            entity.getStatus(), entity.getCreatedAt(), entity.getBio(), entity.getPhone(), entity.getAvatarUrl(),
            entity.getBusinessName(), entity.getWebsite(), entity.getPartnerDescription());
    }
}
