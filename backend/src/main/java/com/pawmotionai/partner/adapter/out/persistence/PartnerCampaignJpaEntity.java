package com.pawmotionai.partner.adapter.out.persistence;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import com.pawmotionai.partner.domain.model.CampaignStatus;

@Entity
@Table(name = "partner_campaigns")
public class PartnerCampaignJpaEntity {

    @Id
    private UUID id;

    @Column(name = "partner_id", nullable = false)
    private UUID partnerId;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "target_audience", nullable = false)
    private String targetAudience;

    @Column(name = "cta", nullable = false)
    private String callToAction;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CampaignStatus status;

    @Column(name = "ai_topics")
    private String aiTopics;

    @Column(name = "analyzed_at")
    private Instant analyzedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    protected PartnerCampaignJpaEntity() {
    }

    public PartnerCampaignJpaEntity(UUID id, UUID partnerId, String title, String description, String targetAudience,
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

    public UUID getId() { return id; }
    public UUID getPartnerId() { return partnerId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public String getTargetAudience() { return targetAudience; }
    public String getCallToAction() { return callToAction; }
    public CampaignStatus getStatus() { return status; }
    public String getAiTopics() { return aiTopics; }
    public Instant getAnalyzedAt() { return analyzedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void update(String title, String description, String targetAudience, String callToAction,
                       CampaignStatus status, String aiTopics, Instant analyzedAt, Instant updatedAt) {
        this.title = title;
        this.description = description;
        this.targetAudience = targetAudience;
        this.callToAction = callToAction;
        this.status = status;
        this.aiTopics = aiTopics;
        this.analyzedAt = analyzedAt;
        this.updatedAt = updatedAt;
    }
}
