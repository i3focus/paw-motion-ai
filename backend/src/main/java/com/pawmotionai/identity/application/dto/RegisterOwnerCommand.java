package com.pawmotionai.identity.application.dto;

public record RegisterOwnerCommand(String email, String password, String displayName) {
}
