package com.pawmotionai.identity.domain.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class OwnerProfileTest {

    @Test
    fun registersOwnerAndPartnerRoles() {
        val owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now())
        val partner = Owner.registerPartner(UUID.randomUUID(), "partner@example.com", "hash", "Partner", Instant.now())

        assertEquals(OwnerRole.OWNER, owner.role)
        assertEquals(OwnerRole.PARTNER, partner.role)
        assertTrue(owner.isActive)
        assertTrue(partner.isPartner)
    }

    @Test
    fun updatesProfileFieldsAndNormalizesEmptyValues() {
        val owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now())

        owner.updateProfile("  Ana  ", "Bio", "", "https://avatar.example")

        assertEquals("Ana", owner.displayName)
        assertEquals("Bio", owner.bio)
        assertNull(owner.phone)
        assertEquals("https://avatar.example", owner.avatarUrl)
        assertFalse(owner.isPartner)
    }

    @Test
    fun keepsOmittedProfileFieldsUnchanged() {
        val owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now())
        owner.updateProfile(null, "Bio", "+55 11 99999-9999", null)

        owner.updateProfile(null, "Updated bio", null, null)

        assertEquals("Owner", owner.displayName)
        assertEquals("Updated bio", owner.bio)
        assertEquals("+55 11 99999-9999", owner.phone)
    }

    @Test
    fun rejectsBlankDisplayName() {
        val owner = Owner.register(UUID.randomUUID(), "owner@example.com", "hash", "Owner", Instant.now())

        assertThrows(IllegalArgumentException::class.java) {
            owner.updateProfile("   ", null, null, null)
        }
    }
}
