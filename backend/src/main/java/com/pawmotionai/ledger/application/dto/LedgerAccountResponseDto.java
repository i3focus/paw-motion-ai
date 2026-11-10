package com.pawmotionai.ledger.application.dto;

import java.util.UUID;

import com.pawmotionai.ledger.domain.model.LedgerAccountCode;
import com.pawmotionai.ledger.domain.model.LedgerAccountStatus;

public record LedgerAccountResponseDto(UUID id, UUID walletId, LedgerAccountCode code, LedgerAccountStatus status,
                                       long balance) {
}
