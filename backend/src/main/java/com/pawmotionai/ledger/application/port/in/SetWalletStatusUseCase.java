package com.pawmotionai.ledger.application.port.in;

import java.util.UUID;

import com.pawmotionai.ledger.application.dto.WalletResponseDto;

public interface SetWalletStatusUseCase {

    WalletResponseDto block(UUID partnerId, String reason);

    WalletResponseDto unblock(UUID partnerId);
}
