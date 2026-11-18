package com.pawmotionai.aiinsights.adapter.out.persistence

import com.pawmotionai.aiinsights.application.port.out.InsightRepository
import com.pawmotionai.aiinsights.domain.model.Insight
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.stereotype.Repository
import java.util.UUID

interface InsightJpaRepository : JpaRepository<InsightJpaEntity, UUID>

@Repository
class InsightPersistenceAdapter(private val repository: InsightJpaRepository) : InsightRepository {

    override fun save(insight: Insight): Insight {
        val entity = InsightJpaEntity(
            insight.id, insight.ownerId, insight.petId, insight.text, insight.provider,
            insight.model, insight.fallbackUsed, insight.generatedAt, insight.generatedAt, insight.generatedAt)
        return toDomain(repository.save(entity))
    }

    private fun toDomain(entity: InsightJpaEntity): Insight =
        Insight.reconstitute(
            entity.id, entity.ownerId, entity.petId, entity.text, entity.provider, entity.model,
            entity.fallbackUsed, entity.generatedAt)
}
