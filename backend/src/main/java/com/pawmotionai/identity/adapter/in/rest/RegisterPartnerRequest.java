package com.pawmotionai.identity.adapter.in.rest;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterPartnerRequest(
    @NotBlank @Email @Size(max = 320) String email,
    @NotBlank @Size(min = 8, max = 128) String password,
    @NotBlank @Size(max = 120) String displayName,
    @NotBlank @Size(max = 160) String businessName,
    @Size(max = 300) String website,
    @Size(max = 1000) String description) {
}
