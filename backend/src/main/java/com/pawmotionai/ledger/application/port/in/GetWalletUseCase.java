package com.pawmotionai.ledger.application.port.in;

import java.util.UUID;

import com.pawmotionai.ledger.application.dto.WalletResponseDto;

public interface GetWalletUseCase {

    WalletResponseDto getByPartner(UUID partnerId);
}
