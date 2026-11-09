package com.pawmotionai.partner.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PartnerCampaign {

    private final UUID id;
    private final UUID partnerId;
    private final String title;
    private final String description;
    private final String targetAudience;
    private final String callToAction;
    private final Instant createdAt;
    private final Instant updatedAt;
    private CampaignStatus status;
    private String aiTopics;
    private Instant analyzedAt;

    private PartnerCampaign(UUID id, UUID partnerId, String title, String description, String targetAudience,
                            String callToAction, CampaignStatus status, String aiTopics, Instant analyzedAt,
                            Instant createdAt, Instant updatedAt) {
        this.id = id;
        this.partnerId = partnerId;
        this.title = title;
        this.description = description;
        this.targetAudience = targetAudience;
        this.callToAction = callToAction;
        this.status = status;
        this.aiTopics = aiTopics;
        this.analyzedAt = analyzedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PartnerCampaign create(UUID id, UUID partnerId, String title, String description,
                                         String targetAudience, String callToAction, Instant createdAt) {
        return new PartnerCampaign(id, partnerId, title.trim(), description.trim(), targetAudience.trim(),
            callToAction.trim(), CampaignStatus.DRAFT, null, null, createdAt, createdAt);
    }

    public static PartnerCampaign reconstitute(UUID id, UUID partnerId, String title, String description,
                                               String targetAudience, String callToAction, CampaignStatus status,
                                               String aiTopics, Instant analyzedAt, Instant createdAt,
                                               Instant updatedAt) {
        return new PartnerCampaign(id, partnerId, title, description, targetAudience, callToAction, status,
            aiTopics, analyzedAt, createdAt, updatedAt);
    }

    public boolean belongsTo(UUID accountId) {
        return partnerId.equals(accountId);
    }

    public void activate() {
        this.status = CampaignStatus.ACTIVE;
    }

    public void pause() {
        this.status = CampaignStatus.PAUSED;
    }

    public void applyAnalysis(List<String> topics, Instant analyzedAt) {
        if (topics == null || topics.isEmpty()) {
            throw new IllegalArgumentException("AI analysis must return at least one topic");
        }
        this.aiTopics = String.join("\n", topics.stream().map(String::trim).filter(topic -> !topic.isEmpty()).toList());
        this.analyzedAt = analyzedAt;
        this.status = CampaignStatus.ACTIVE;
    }

    public List<String> topics() {
        if (aiTopics == null || aiTopics.isBlank()) {
            return List.of();
        }
        return aiTopics.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
    }

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTargetAudience() { return targetAudience; }
    public String getCallToAction() { return callToAction; }
    public CampaignStatus getStatus() { return status; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
