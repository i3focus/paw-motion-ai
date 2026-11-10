package com.pawmotionai.ledger.domain.exception;

public class InsufficientCreditsException extends RuntimeException {

    private final long requiredCredits;
    private final long availableBalance;

    public InsufficientCreditsException(long requiredCredits, long availableBalance) {
        super("Insufficient credits: required " + requiredCredits + ", available " + availableBalance);
        this.requiredCredits = requiredCredits;
        this.availableBalance = availableBalance;
    }

    public long getRequiredCredits() { return requiredCredits; }
    public long getAvailableBalance() { return availableBalance; }
}
