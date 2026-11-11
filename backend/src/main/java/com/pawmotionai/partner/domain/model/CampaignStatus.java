package com.pawmotionai.partner.domain.model;

public enum CampaignStatus {

    DRAFT,
    IN_REVIEW,
    ACTIVE,
    PAUSED,
    ENDED;

    public static CampaignStatus fromValue(String value) {
        if (value == null || value.isBlank()) {
            return DRAFT;
        }
        String normalized = value.trim().toUpperCase();
        return "ARCHIVED".equals(normalized) || "EXPIRED".equals(normalized) ? ENDED : valueOf(normalized);
    }

    public boolean isVisibleToOwners() {
        return this == ACTIVE;
    }
}
