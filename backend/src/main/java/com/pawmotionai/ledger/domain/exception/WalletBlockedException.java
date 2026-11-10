package com.pawmotionai.ledger.domain.exception;

public class WalletBlockedException extends RuntimeException {

    public WalletBlockedException(String reason) {
        super(reason == null || reason.isBlank() ? "Wallet is blocked" : reason);
    }
}
