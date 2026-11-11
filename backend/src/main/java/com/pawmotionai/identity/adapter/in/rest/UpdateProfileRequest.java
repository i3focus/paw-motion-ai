package com.pawmotionai.identity.adapter.in.rest;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;

public record UpdateProfileRequest(
    @Size(max = 120) String displayName,
    @Size(max = 500) String bio,
    @Size(max = 40) String phone,
    @Size(max = 500) String avatarUrl,
    @Size(max = 160) String businessName,
    @Size(max = 300) String website,
    @Size(max = 1000) String partnerDescription,
    @DecimalMin("-90") @DecimalMax("90") Double latitude,
    @DecimalMin("-180") @DecimalMax("180") Double longitude,
    Boolean clearCoordinates) {
}
