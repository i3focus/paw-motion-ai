package com.pawmotionai.ledger.domain.exception;

import java.util.UUID;

public class WalletNotFoundException extends RuntimeException {

    public WalletNotFoundException(UUID partnerId) {
        super("Wallet not found for partner " + partnerId);
    }
}
