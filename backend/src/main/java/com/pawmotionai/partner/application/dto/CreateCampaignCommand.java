package com.pawmotionai.partner.application.dto;

import java.util.UUID;

public record CreateCampaignCommand(UUID partnerId, String title, String description, String targetAudience,
                                    String callToAction, String idempotencyKey) {
}
