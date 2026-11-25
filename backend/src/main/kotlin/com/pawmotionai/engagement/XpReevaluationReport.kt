package com.pawmotionai.engagement

import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * C6: dry-run report of XP already granted from sources the policy now excludes (days above the cap, same-owner
 * playdates, repeated challenge rewards). Never changes `xp_events`: any adjustment is a manual, separately approved decision.
 */
@Component
class XpReevaluationReport(
    private val jdbc: JdbcClient,
    private val engagement: EngagementService,
    @Value("\${app.xp.reevaluation-report.enabled:false}") private val enabled: Boolean,
) {
    private val log = LoggerFactory.getLogger(XpReevaluationReport::class.java)

    data class Report(val petDaysOverCap: Int, val excessOverCapPoints: Long, val sameOwnerPlaydatePoints: Long, val duplicateChallengeRewards: Long)

    @Scheduled(cron = "\${app.xp.reevaluation-report.cron:0 30 4 * * *}")
    fun scheduled() {
        if (enabled) report()
    }

    fun report(): Report {
        val perDay = jdbc.sql(
            """
            SELECT source_type, SUM(points) AS total FROM xp_events
            GROUP BY pet_id, source_type, (occurred_at AT TIME ZONE 'UTC')::date
            """,
        ).query { rs, _ -> rs.getString(1) to rs.getLong(2) }.list()
        val over = perDay.filter { (type, total) -> total > engagement.dailyCap(type) }
        val excess = over.sumOf { (type, total) -> total - engagement.dailyCap(type) }
        val samePlaydate = jdbc.sql(
            """
            SELECT COALESCE(SUM(x.points), 0) FROM xp_events x
            WHERE x.source_type = 'PLAYDATE'
              AND NOT EXISTS (SELECT 1 FROM playdates d WHERE d.id::text = split_part(x.source_key, ':', 2) AND d.organizer_owner_id <> x.owner_id)
              AND NOT EXISTS (SELECT 1 FROM playdate_invites i WHERE i.playdate_id::text = split_part(x.source_key, ':', 2) AND i.invitee_owner_id <> x.owner_id)
            """,
        ).query(Long::class.java).single()
        val duplicates = jdbc.sql(
            """
            SELECT COALESCE(SUM(n - 1), 0) FROM (
                SELECT COUNT(*) AS n FROM xp_events WHERE source_type = 'CHALLENGE'
                GROUP BY owner_id, split_part(source_key, ':', 2)
            ) t WHERE n > 1
            """,
        ).query(Long::class.java).single()
        log.info("xp reevaluation (dry-run, nothing removed): petDaysOverCap={} excessPoints={} sameOwnerPlaydatePoints={} duplicateChallengeRewards={}",
            over.size, excess, samePlaydate, duplicates)
        return Report(over.size, excess, samePlaydate, duplicates)
    }
}
