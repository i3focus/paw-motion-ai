package com.pawmotionai.aiinsights.adapter.out.persistence;

import org.springframework.stereotype.Repository;

import com.pawmotionai.aiinsights.application.port.out.InsightRepository;
import com.pawmotionai.aiinsights.domain.model.Insight;

@Repository
public class InsightPersistenceAdapter implements InsightRepository {

    private final InsightJpaRepository repository;

    public InsightPersistenceAdapter(InsightJpaRepository repository) {
        this.repository = repository;
    }

    @Override
    public Insight save(Insight insight) {
        InsightJpaEntity entity = new InsightJpaEntity(
            insight.getId(), insight.getOwnerId(), insight.getPetId(), insight.getText(), insight.getProvider(),
            insight.getModel(), insight.isFallbackUsed(), insight.getGeneratedAt(), insight.getGeneratedAt(), insight.getGeneratedAt());
        return toDomain(repository.save(entity));
    }

    private Insight toDomain(InsightJpaEntity entity) {
        return Insight.reconstitute(
            entity.getId(), entity.getOwnerId(), entity.getPetId(), entity.getText(), entity.getProvider(), entity.getModel(),
            entity.isFallbackUsed(), entity.getGeneratedAt());
    }
}
