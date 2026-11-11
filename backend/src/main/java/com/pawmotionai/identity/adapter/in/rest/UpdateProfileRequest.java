package com.pawmotionai.identity.adapter.in.rest;

import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 120) String displayName,
    @Size(max = 500) String bio,
    @Size(max = 40) String phone,
    @Size(max = 500) String avatarUrl) {
}
