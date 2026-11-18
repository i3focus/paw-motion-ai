package com.pawmotionai.shared

/**
 * Granted-authority names used by `@Secured` annotations. Values mirror
 * [com.pawmotionai.identity.domain.model.OwnerRole] and are prefixed with
 * `ROLE_` to match the authority prefix applied by the JWT converter.
 */
object SecurityRoles {
    const val OWNER = "ROLE_OWNER"
    const val PARTNER = "ROLE_PARTNER"
    const val ADMIN = "ROLE_ADMIN"
}
