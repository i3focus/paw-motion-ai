package com.pawmotionai.ledger.application.dto;

import java.util.List;
import java.util.UUID;

import com.pawmotionai.ledger.domain.model.WalletStatus;

public record WalletResponseDto(UUID walletId, UUID partnerId, WalletStatus status, String blockedReason,
                                List<LedgerAccountResponseDto> accounts, long totalBalance) {
}
