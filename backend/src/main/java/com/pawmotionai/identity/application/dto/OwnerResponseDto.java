package com.pawmotionai.identity.application.dto;

import java.util.UUID;

public record OwnerResponseDto(UUID id, String email, String displayName, String role, String bio, String phone,
                               String avatarUrl, String businessName, String website, String partnerDescription) {

    public static OwnerResponseDto from(com.pawmotionai.identity.domain.model.Owner owner) {
        return new OwnerResponseDto(owner.getId(), owner.getEmail(), owner.getDisplayName(), owner.getRole(), owner.getBio(),
            owner.getPhone(), owner.getAvatarUrl(), owner.getBusinessName(), owner.getWebsite(), owner.getPartnerDescription());
    }
}
