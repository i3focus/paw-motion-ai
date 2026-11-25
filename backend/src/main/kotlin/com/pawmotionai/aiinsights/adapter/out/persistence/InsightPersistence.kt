package com.pawmotionai.aiinsights.adapter.out.persistence

import com.pawmotionai.aiinsights.application.port.out.InsightRepository
import com.pawmotionai.aiinsights.domain.model.Insight
import io.micrometer.core.instrument.MeterRegistry
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.stereotype.Repository
import java.sql.Timestamp
import java.time.ZoneOffset
import java.util.UUID

/**
 * C13: save is an upsert by (owner, pet, UTC day) instead of a permanent insert per call.
 * Legacy rows (bucket_date NULL) are left untouched.
 */
@Repository
class InsightPersistenceAdapter(
    private val jdbc: JdbcTemplate,
    private val meterRegistry: MeterRegistry? = null,
) : InsightRepository {

    override fun save(insight: Insight): Insight {
        val bucket = insight.generatedAt.atZone(ZoneOffset.UTC).toLocalDate()
        val now = Timestamp.from(insight.generatedAt)
        val id = jdbc.queryForObject(
            """
            INSERT INTO insights (id, owner_id, pet_id, text, provider, model, fallback_used, generated_at,
                                  created_at, updated_at, version, bucket_date)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, 0, ?)
            ON CONFLICT (owner_id, (COALESCE(pet_id, '00000000-0000-0000-0000-000000000000'::uuid)), bucket_date)
                WHERE bucket_date IS NOT NULL
            DO UPDATE SET text = EXCLUDED.text, provider = EXCLUDED.provider, model = EXCLUDED.model,
                          fallback_used = EXCLUDED.fallback_used, generated_at = EXCLUDED.generated_at,
                          updated_at = EXCLUDED.updated_at, version = insights.version + 1
            RETURNING id
            """.trimIndent(),
            UUID::class.java,
            insight.id, insight.ownerId, insight.petId, insight.text, insight.provider, insight.model,
            insight.fallbackUsed, now, now, now, java.sql.Date.valueOf(bucket))!!
        meterRegistry?.counter("insights.upserts", "result", if (id == insight.id) "inserted" else "updated")?.increment()
        return Insight.reconstitute(
            id, insight.ownerId, insight.petId, insight.text, insight.provider, insight.model,
            insight.fallbackUsed, insight.generatedAt)
    }
}
