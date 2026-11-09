package com.pawmotionai.identity.domain.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Instant;
import java.util.UUID;

import org.junit.jupiter.api.Test;

class OwnerProfileTest {

    @Test
    void registersOwnerAndPartnerRoles() {
        Owner owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now());
        Owner partner = Owner.registerPartner(UUID.randomUUID(), "partner@example.com", "hash", "Partner",
            "PetCo", "https://petco.example", "Pet supplies", Instant.now());

        assertEquals("OWNER", owner.getRole());
        assertEquals("PARTNER", partner.getRole());
        assertTrue(owner.isActive());
        assertTrue(partner.isPartner());
    }

    @Test
    void updatesProfileFieldsAndNormalizesEmptyValues() {
        Owner owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now());

        owner.updateProfile("  Ana  ", "Bio", "", "https://avatar.example", null, null, null);

        assertEquals("Ana", owner.getDisplayName());
        assertEquals("Bio", owner.getBio());
        assertNull(owner.getPhone());
        assertEquals("https://avatar.example", owner.getAvatarUrl());
        assertFalse(owner.isPartner());
    }
}
