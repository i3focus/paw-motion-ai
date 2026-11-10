package com.pawmotionai.ledger.adapter.out.persistence;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface LedgerWalletJpaRepository extends JpaRepository<LedgerWalletJpaEntity, UUID> {

    Optional<LedgerWalletJpaEntity> findByPartnerId(UUID partnerId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select w from LedgerWalletJpaEntity w where w.partnerId = :partnerId")
    Optional<LedgerWalletJpaEntity> findByPartnerIdForUpdate(@Param("partnerId") UUID partnerId);
}
