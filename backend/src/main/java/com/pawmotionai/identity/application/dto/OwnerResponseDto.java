package com.pawmotionai.identity.application.dto;

import java.util.UUID;

public record OwnerResponseDto(UUID id, String email, String displayName) {

    public static OwnerResponseDto from(com.pawmotionai.identity.domain.model.Owner owner) {
        return new OwnerResponseDto(owner.getId(), owner.getEmail(), owner.getDisplayName());
    }
}
