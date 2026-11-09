package com.pawmotionai.identity.application.dto;

public record AuthResponseDto(String accessToken, String tokenType, long expiresInSeconds, OwnerResponseDto owner) {
}
