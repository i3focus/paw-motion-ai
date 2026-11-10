package com.pawmotionai.partner.domain.model;

public enum CampaignStatus {

    DRAFT,
    ACTIVE,
    PAUSED,
    ARCHIVED,
    EXPIRED;

    public static CampaignStatus fromValue(String value) {
        return value == null || value.isBlank() ? DRAFT : valueOf(value.trim().toUpperCase());
    }

    public boolean isVisibleToOwners() {
        return this == ACTIVE;
    }
}
