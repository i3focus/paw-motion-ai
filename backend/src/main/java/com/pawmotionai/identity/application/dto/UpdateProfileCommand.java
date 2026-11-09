package com.pawmotionai.identity.application.dto;

public record UpdateProfileCommand(String displayName, String bio, String phone, String avatarUrl,
                                   String businessName, String website, String partnerDescription) {
}
