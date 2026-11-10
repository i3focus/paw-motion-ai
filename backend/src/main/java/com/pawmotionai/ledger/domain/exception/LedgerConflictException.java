package com.pawmotionai.ledger.domain.exception;

public class LedgerConflictException extends RuntimeException {

    public LedgerConflictException(String message) {
        super(message);
    }
}
