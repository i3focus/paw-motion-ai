package com.pawmotionai.partner.adapter.in.rest;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateCampaignRequest(
    @NotBlank @Size(max = 160) String title,
    @NotBlank @Size(max = 2000) String description,
    @NotBlank @Size(max = 160) String targetAudience,
    @NotBlank @Size(max = 160) String callToAction) {
}
