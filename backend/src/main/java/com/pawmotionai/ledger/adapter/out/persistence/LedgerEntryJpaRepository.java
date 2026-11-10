package com.pawmotionai.ledger.adapter.out.persistence;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.pawmotionai.ledger.domain.model.LedgerEntryDirection;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;

public interface LedgerEntryJpaRepository extends JpaRepository<LedgerEntryJpaEntity, UUID> {

    List<LedgerEntryJpaEntity> findByReferenceTypeAndReferenceIdAndDirection(LedgerReferenceType referenceType,
                                                                            UUID referenceId,
                                                                            LedgerEntryDirection direction);

    List<LedgerEntryJpaEntity> findByAccountIdInOrderByOccurredAtDesc(List<UUID> accountIds, Pageable pageable);

    long countByAccountIdIn(List<UUID> accountIds);
}
