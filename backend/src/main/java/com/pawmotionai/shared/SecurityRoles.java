package com.pawmotionai.shared;

/**
 * Granted-authority names used by {@code @Secured} annotations. Values mirror
 * {@link com.pawmotionai.identity.domain.model.OwnerRole} and are prefixed with
 * {@code ROLE_} to match the authority prefix applied by the JWT converter.
 */
public final class SecurityRoles {

    public static final String OWNER = "ROLE_OWNER";
    public static final String PARTNER = "ROLE_PARTNER";
    public static final String ADMIN = "ROLE_ADMIN";

    private SecurityRoles() {
    }
}
