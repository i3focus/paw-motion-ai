package com.pawmotionai.identity.adapter.out.persistence

import com.pawmotionai.identity.application.port.out.OwnerRepository
import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException
import com.pawmotionai.identity.domain.model.Owner
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Repository
import java.time.Instant
import java.util.UUID

@Repository
class OwnerPersistenceAdapter(private val repository: OwnerJpaRepository) : OwnerRepository {

    override fun save(owner: Owner): Owner {
        val entity = repository.findById(owner.id)
            .orElseGet {
                OwnerJpaEntity(
                    owner.id, owner.email, owner.passwordHash, owner.displayName, owner.role,
                    owner.status, owner.createdAt, Instant.now(), null, null, null)
            }
        entity.updateProfile(owner.displayName, owner.bio, owner.phone, owner.avatarUrl)
        try {
            return toDomain(repository.saveAndFlush(entity))
        } catch (exception: DataIntegrityViolationException) {
            if (isEmailUniqueViolation(exception)) {
                throw OwnerAlreadyExistsException(owner.email)
            }
            throw exception
        }
    }

    override fun findById(id: UUID): Owner? = repository.findById(id).map(::toDomain).orElse(null)

    override fun findByEmailIgnoreCase(email: String): Owner? = repository.findByEmailIgnoreCase(email)?.let(::toDomain)

    override fun existsByEmailIgnoreCase(email: String): Boolean = repository.existsByEmailIgnoreCase(email)

    private fun isEmailUniqueViolation(exception: DataIntegrityViolationException): Boolean {
        val message = exception.message
        return message != null && (message.contains("uq_owners_email") || message.contains("duplicate key value violates unique constraint"))
    }

    private fun toDomain(entity: OwnerJpaEntity): Owner =
        Owner.reconstitute(
            entity.id, entity.email, entity.passwordHash, entity.displayName, entity.role,
            entity.status, entity.createdAt, entity.bio, entity.phone, entity.avatarUrl)
}
