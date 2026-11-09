package com.pawmotionai.identity.domain.exception;

import java.util.UUID;

public class OwnerNotFoundException extends RuntimeException {

    public OwnerNotFoundException(UUID ownerId) {
        super("Owner not found: " + ownerId);
    }
}
