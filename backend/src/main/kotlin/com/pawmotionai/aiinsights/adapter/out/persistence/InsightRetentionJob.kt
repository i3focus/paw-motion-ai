package com.pawmotionai.aiinsights.adapter.out.persistence

import io.micrometer.core.instrument.MeterRegistry
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant

/** C13: optional TTL. Disabled by default (app.insights.retention.enabled=false); only deletes in bounded batches. */
@Component
@ConditionalOnProperty(name = ["app.insights.retention.enabled"], havingValue = "true")
class InsightRetentionJob(
    private val jdbc: JdbcTemplate,
    @Value("\${app.insights.retention.days:365}") private val days: Long,
    @Value("\${app.insights.retention.batch-size:1000}") private val batchSize: Int,
    private val meterRegistry: MeterRegistry? = null,
) {
    @Scheduled(cron = "\${app.insights.retention.cron:0 30 3 * * *}")
    fun purgeExpired() {
        val cutoff = Timestamp.from(Instant.now().minus(Duration.ofDays(days)))
        val deleted = jdbc.update(
            "DELETE FROM insights WHERE id IN (SELECT id FROM insights WHERE generated_at < ? LIMIT ?)", cutoff, batchSize)
        meterRegistry?.counter("insights.retention.deleted")?.increment(deleted.toDouble())
        log.info("Insight retention removed {} rows older than {} days", deleted, days)
    }

    private companion object {
        val log = LoggerFactory.getLogger(InsightRetentionJob::class.java)
    }
}
