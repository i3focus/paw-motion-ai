package com.pawmotionai.ledger.application.port.in;

import java.util.UUID;

public interface EnsureWalletUseCase {

    void ensureWallet(UUID partnerId);
}
