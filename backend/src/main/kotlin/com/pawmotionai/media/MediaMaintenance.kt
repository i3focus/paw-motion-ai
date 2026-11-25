package com.pawmotionai.media

import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.concurrent.atomic.AtomicInteger

/** C10: deletes files after the commit that removed their `media_assets` row (outbox filled by a DB trigger). */
@Component
class MediaDeletionOutbox(
    private val jdbc: JdbcClient,
    private val storage: MediaStoragePort,
    private val tx: TransactionTemplate,
    private val meterRegistry: MeterRegistry,
    @Value("\${app.media.deletion.enabled:true}") private val enabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(MediaDeletionOutbox::class.java)

    @Scheduled(fixedDelayString = "\${app.media.deletion.interval-ms:60000}", initialDelayString = "\${app.media.deletion.initial-delay-ms:30000}")
    fun scheduledDrain() {
        if (enabled) drain()
    }

    /** Processes one batch; returns how many files were removed. */
    fun drain(batch: Int = 100): Int = tx.execute {
        val pending = jdbc.sql(
            "SELECT id, storage_key FROM media_deletions WHERE processed_at IS NULL ORDER BY id LIMIT :limit FOR UPDATE SKIP LOCKED",
        ).param("limit", batch).query { rs, _ -> rs.getLong(1) to rs.getString(2) }.list()
        var done = 0
        for ((id, key) in pending) {
            try {
                storage.delete(key)
                jdbc.sql("UPDATE media_deletions SET processed_at = now(), attempts = attempts + 1 WHERE id = :id").param("id", id).update()
                done++
            } catch (e: Exception) {
                log.warn("media file deletion failed outboxId={} error={}", id, e.javaClass.simpleName)
                jdbc.sql("UPDATE media_deletions SET attempts = attempts + 1, last_error = :err WHERE id = :id")
                    .param("err", e.javaClass.simpleName).param("id", id).update()
            }
        }
        if (done > 0) meterRegistry.counter("media.files_deleted").increment(done.toDouble())
        done
    } ?: 0
}

/** C10: dry-run reconciliation between storage and `media_assets`. Only reports; never deletes files or rows. Off by default. */
@Component
class MediaOrphanSweep(
    private val jdbc: JdbcClient,
    private val storage: MediaStoragePort,
    private val meterRegistry: MeterRegistry,
    @Value("\${app.media.orphan-sweep.enabled:false}") private val enabled: Boolean,
    @Value("\${app.media.orphan-sweep.grace:1h}") private val grace: Duration,
    @Value("\${app.media.orphan-sweep.max-scan:10000}") private val maxScan: Int,
) {
    private val log = LoggerFactory.getLogger(MediaOrphanSweep::class.java)

    private val orphanFileGauge = AtomicInteger(0).also { meterRegistry.gauge("media.orphans", listOf(Tag.of("kind", "file")), it) }
    private val missingRowGauge = AtomicInteger(0).also { meterRegistry.gauge("media.orphans", listOf(Tag.of("kind", "row")), it) }

    data class Report(val orphanFiles: Int, val missingFiles: Int)

    @Scheduled(fixedDelayString = "\${app.media.orphan-sweep.interval-ms:21600000}", initialDelayString = "\${app.media.orphan-sweep.initial-delay-ms:300000}")
    fun scheduledSweep() {
        if (enabled) sweep()
    }

    fun sweep(): Report {
        val keys = storage.listOlderThan(Instant.now().minus(grace), maxScan)
        val known = keys.chunked(500).flatMap { chunk ->
            jdbc.sql("SELECT storage_key FROM media_assets WHERE storage_key IN (:keys)").param("keys", chunk).query(String::class.java).list()
        }.toSet()
        val pendingDeletion = keys.chunked(500).flatMap { chunk ->
            jdbc.sql("SELECT storage_key FROM media_deletions WHERE processed_at IS NULL AND storage_key IN (:keys)").param("keys", chunk).query(String::class.java).list()
        }.toSet()
        val orphanFiles = keys.count { it !in known && it !in pendingDeletion }
        val missing = jdbc.sql("SELECT storage_key FROM media_assets ORDER BY created_at DESC LIMIT :limit").param("limit", maxScan)
            .query { rs, _ -> rs.getString(1) }.list().count { !storage.exists(it) }
        orphanFileGauge.set(orphanFiles)
        missingRowGauge.set(missing)
        log.info("media orphan sweep (dry-run): orphanFiles={} rowsWithoutFile={} scanned={}", orphanFiles, missing, keys.size)
        return Report(orphanFiles, missing)
    }
}
