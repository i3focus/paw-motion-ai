package com.pawmotionai.identity.application.dto;

public record RegisterPartnerCommand(String email, String password, String displayName, String businessName,
                                     String website, String description) {
}
