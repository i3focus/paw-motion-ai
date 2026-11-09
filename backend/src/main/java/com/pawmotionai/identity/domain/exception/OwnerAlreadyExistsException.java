package com.pawmotionai.identity.domain.exception;

public class OwnerAlreadyExistsException extends RuntimeException {

    public OwnerAlreadyExistsException(String email) {
        super("Owner already exists for email: " + email);
    }
}
