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
        Owner partner = Owner.registerPartner(UUID.randomUUID(), "partner@example.com", "hash", "Partner", Instant.now());

        assertEquals("OWNER", owner.getRole());
        assertEquals("PARTNER", partner.getRole());
        assertTrue(owner.isActive());
        assertTrue(partner.isPartner());
    }

    @Test
    void updatesProfileFieldsAndNormalizesEmptyValues() {
        Owner owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now());

        owner.updateProfile("  Ana  ", "Bio", "", "https://avatar.example");

        assertEquals("Ana", owner.getDisplayName());
        assertEquals("Bio", owner.getBio());
        assertNull(owner.getPhone());
        assertEquals("https://avatar.example", owner.getAvatarUrl());
        assertFalse(owner.isPartner());
    }

    @Test
    void keepsOmittedProfileFieldsUnchanged() {
        Owner owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now());
        owner.updateProfile(null, "Bio", "+55 11 99999-9999", null);

        owner.updateProfile(null, "Updated bio", null, null);

        assertEquals("Owner", owner.getDisplayName());
        assertEquals("Updated bio", owner.getBio());
        assertEquals("+55 11 99999-9999", owner.getPhone());
    }

    @Test
    void rejectsBlankDisplayName() {
        Owner owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now());

        org.junit.jupiter.api.Assertions.assertThrows(IllegalArgumentException.class,
            () -> owner.updateProfile("   ", null, null, null));
    }
}
