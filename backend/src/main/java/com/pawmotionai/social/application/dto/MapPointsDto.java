package com.pawmotionai.social.application.dto;

import java.util.List;
import java.util.UUID;

public record MapPointsDto(List<PartnerMapPointDto> partners, List<ActivityMapPointDto> activities) {
    public record PartnerMapPointDto(UUID id, String name, String avatarUrl, String website, Double latitude, Double longitude) {
    }

    public record ActivityMapPointDto(UUID id, UUID petId, String petName, String type, Double latitude, Double longitude,
                                      java.time.Instant startedAt) {
    }
}
