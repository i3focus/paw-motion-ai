package com.pawmotionai.ledger.application.dto;

import java.time.Instant;
import java.util.UUID;

import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerEntryDirection;
import com.pawmotionai.ledger.domain.model.LedgerReferenceType;

public record LedgerEntryResponseDto(UUID id, UUID accountId, LedgerAccountCode accountCode,
                                     LedgerEntryDirection direction, long amount, long balanceAfter,
                                     LedgerReferenceType referenceType, UUID referenceId, String description,
                                     Instant occurredAt) {
}
