package com.pawmotionai.ledger.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.QueryHints;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;
import jakarta.persistence.QueryHint;

public interface LedgerAccountJpaRepository extends JpaRepository<LedgerAccountJpaEntity, UUID> {

    List<LedgerAccountJpaEntity> findByWalletIdOrderByDebitPriorityAsc(UUID walletId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @QueryHints(@QueryHint(name = "jakarta.persistence.lock.timeout", value = "2000"))
    @Query("select a from LedgerAccountJpaEntity a where a.walletId = :walletId order by a.debitPriority asc")
    List<LedgerAccountJpaEntity> findByWalletIdForUpdateOrderByDebitPriorityAsc(@Param("walletId") UUID walletId);
}
