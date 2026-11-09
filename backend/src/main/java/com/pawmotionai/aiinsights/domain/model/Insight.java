package com.pawmotionai.aiinsights.domain.model;

import java.time.Instant;
import java.util.UUID;

public class Insight {

    private final UUID id;
    private final UUID ownerId;
    private final UUID petId;
    private final String text;
    private final String provider;
    private final String model;
    private final boolean fallbackUsed;
    private final Instant generatedAt;

    private Insight(UUID id, UUID ownerId, UUID petId, String text, String provider, String model,
                    boolean fallbackUsed, Instant generatedAt) {
        this.id = id;
        this.ownerId = ownerId;
        this.petId = petId;
        this.text = text;
        this.provider = provider;
        this.model = model;
        this.fallbackUsed = fallbackUsed;
        this.generatedAt = generatedAt;
    }

    public static Insight generated(UUID id, UUID ownerId, UUID petId, String text, String provider, String model,
                                    boolean fallbackUsed, Instant generatedAt) {
        return new Insight(id, ownerId, petId, text, provider, model, fallbackUsed, generatedAt);
    }

    public static Insight reconstitute(UUID id, UUID ownerId, UUID petId, String text, String provider, String model,
                                       boolean fallbackUsed, Instant generatedAt) {
        return new Insight(id, ownerId, petId, text, provider, model, fallbackUsed, generatedAt);
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public UUID getPetId() { return petId; }
    public String getText() { return text; }
    public String getProvider() { return provider; }
    public String getModel() { return model; }
    public boolean isFallbackUsed() { return fallbackUsed; }
    public Instant getGeneratedAt() { return generatedAt; }
}
