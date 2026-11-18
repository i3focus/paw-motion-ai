package com.pawmotionai.identity.adapter.out.persistence

import com.pawmotionai.identity.domain.exception.OwnerAlreadyExistsException
import com.pawmotionai.identity.domain.model.Owner
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.springframework.dao.DataIntegrityViolationException
import java.time.Instant
import java.util.Optional
import java.util.UUID

class OwnerPersistenceAdapterTest {

    @Test
    fun mapsConcurrentUniqueViolationToOwnerAlreadyExists() {
        val jpaRepository = mock(OwnerJpaRepository::class.java)
        val adapter = OwnerPersistenceAdapter(jpaRepository)
        val owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now())

        `when`(jpaRepository.findById(owner.id)).thenReturn(Optional.empty())
        `when`(jpaRepository.saveAndFlush(any(OwnerJpaEntity::class.java)))
            .thenThrow(DataIntegrityViolationException("ERROR: duplicate key value violates unique constraint \"uq_owners_email\""))

        assertThrows(OwnerAlreadyExistsException::class.java) { adapter.save(owner) }
    }

    @Test
    fun rethrowsNonDuplicateIntegrityViolations() {
        val jpaRepository = mock(OwnerJpaRepository::class.java)
        val adapter = OwnerPersistenceAdapter(jpaRepository)
        val owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now())

        `when`(jpaRepository.findById(owner.id)).thenReturn(Optional.empty())
        `when`(jpaRepository.saveAndFlush(any(OwnerJpaEntity::class.java)))
            .thenThrow(DataIntegrityViolationException("value too long for type character varying(320)"))

        assertThrows(DataIntegrityViolationException::class.java) { adapter.save(owner) }
    }
}
