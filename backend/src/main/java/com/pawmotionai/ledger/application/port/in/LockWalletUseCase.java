package com.pawmotionai.ledger.application.port.in;

import java.util.UUID;

public interface LockWalletUseCase {

    void lock(UUID partnerId);
}
