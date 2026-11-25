package com.pawmotionai.shared

import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Component
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

/**
 * Monthly AI quota shared by the assistant and insights (C17, N12). Consumption is a single atomic upsert, so concurrent
 * requests can never push `ai_usage.messages` past the free limit.
 */
@Component
class AiQuota(private val jdbc: JdbcClient) {

    companion object {
        const val FREE_MONTHLY_MESSAGES = 10
    }

    /** Consumes one unit of the current month's quota or throws [QuotaExceededException]. Joins the caller's transaction. */
    fun consume(ownerId: UUID) {
        val plan = jdbc.sql("SELECT plan FROM owners WHERE id = :id").param("id", ownerId).query(String::class.java).single()
        val period = YearMonth.now(ZoneOffset.UTC).toString()
        if (plan == "PREMIUM") {
            jdbc.sql(
                "INSERT INTO ai_usage (owner_id, period, messages) VALUES (:owner, :period, 1) " +
                    "ON CONFLICT (owner_id, period) DO UPDATE SET messages = ai_usage.messages + 1",
            ).param("owner", ownerId).param("period", period).update()
            return
        }
        val granted = jdbc.sql(
            "INSERT INTO ai_usage (owner_id, period, messages) VALUES (:owner, :period, 1) " +
                "ON CONFLICT (owner_id, period) DO UPDATE SET messages = ai_usage.messages + 1 " +
                "WHERE ai_usage.messages < :limit RETURNING messages",
        ).param("owner", ownerId).param("period", period).param("limit", FREE_MONTHLY_MESSAGES)
            .query(Int::class.java).optional()
        if (granted.isEmpty) {
            throw QuotaExceededException(
                "Free plan includes $FREE_MONTHLY_MESSAGES AI messages and insights per month. Upgrade to Premium for unlimited use.",
            )
        }
    }
}
