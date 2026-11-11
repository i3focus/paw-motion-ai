package com.pawmotionai.ads

import com.pawmotionai.media.MediaService
import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.ConflictException
import com.pawmotionai.shared.ForbiddenException
import com.pawmotionai.shared.MediaUrls
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.sql.ResultSet
import java.sql.Timestamp
import java.time.Instant
import java.util.UUID

/**
 * Campaign v2 management (draft → review → serve → end), partner business profile and admin moderation
 * (refactor plan §4.2 `ads`). Runs on JdbcClient over `partner_campaigns`/`partner_profiles`/`campaign_events`;
 * the legacy Java module keeps create/analyze/archive, ledger debits and expiry.
 */
@Service
class AdsService(private val jdbc: JdbcClient, private val media: MediaService) {

    private val log = LoggerFactory.getLogger(AdsService::class.java)

    // ---- partner-facing campaign management ----

    @Transactional(readOnly = true)
    fun campaign(partnerId: UUID, campaignId: UUID): CampaignV2Dto {
        if (partnerOf(campaignId) != partnerId) throw ForbiddenException("Campaign belongs to another partner")
        return jdbc.sql("$CAMPAIGN_SELECT WHERE c.id = :id").param("id", campaignId).query { rs, _ -> campaign(rs) }
            .optional().orElseThrow { NotFoundException("Campaign not found") }
    }

    @Transactional
    fun update(partnerId: UUID, campaignId: UUID, request: UpdateCampaignRequest): CampaignV2Dto {
        val status = statusOf(campaignId)
        if (partnerOf(campaignId) != partnerId) throw ForbiddenException("Campaign belongs to another partner")
        if (status !in setOf("DRAFT", "IN_REVIEW", "PAUSED")) {
            throw ConflictException("Campaigns can only be edited while draft, in review or paused (current: ${status.lowercase()})")
        }
        if (request.serviceId != null) {
            val owner = jdbc.sql("SELECT partner_id FROM partner_services WHERE id = :id").param("id", request.serviceId)
                .query(UUID::class.java).optional().orElseThrow { BadRequestException("Service not found") }
            if (owner != partnerId) throw ForbiddenException("Service belongs to another partner")
        }
        if (request.mediaAssetId != null && !media.ownsAll(partnerId, listOf(request.mediaAssetId))) {
            throw ForbiddenException("Media must be uploaded by the partner")
        }
        if (request.ctaType == CampaignCtaType.BOOKING && request.serviceId == null
            && jdbc.sql("SELECT service_id FROM partner_campaigns WHERE id = :id").param("id", campaignId).query(UUID::class.java).optional().isEmpty) {
            throw BadRequestException("A booking CTA needs serviceId")
        }
        val sets = mutableListOf<String>()
        val params = mutableMapOf<String, Any?>()
        fun set(column: String, value: Any?) { sets += "$column = :$column"; params[column] = value }
        request.title?.let { set("title", it.trim()) }
        request.description?.let { set("description", it.trim()) }
        request.targetAudience?.let { set("target_audience", it.trim()) }
        request.ctaLabel?.let { set("cta", it.trim()) }
        request.ctaType?.let { set("cta_type", it.name) }
        request.ctaUrl?.let { set("cta_url", it.trim()) }
        request.serviceId?.let { set("service_id", it) }
        request.mediaAssetId?.let { set("media_asset_id", it) }
        request.targetSpecies?.let { set("target_species", it.map(String::uppercase).toTypedArray()) }
        request.targetBreeds?.let { set("target_breeds", it.toTypedArray()) }
        request.targetInterests?.let { set("target_interests", it.map(String::lowercase).toTypedArray()) }
        request.formats?.let { set("formats", it.distinct().map(AdFormat::name).toTypedArray()) }
        if (request.startsAt != null || request.endsAt != null) {
            val current = jdbc.sql("SELECT starts_at, ends_at FROM partner_campaigns WHERE id = :id").param("id", campaignId)
                .query { rs, _ -> rs.getTimestamp(1).toInstant() to rs.getTimestamp(2).toInstant() }.single()
            val startsAt = request.startsAt ?: current.first
            val endsAt = request.endsAt ?: current.second
            if (!endsAt.isAfter(startsAt)) throw BadRequestException("endsAt must be after startsAt")
            set("starts_at", Timestamp.from(startsAt)); set("ends_at", Timestamp.from(endsAt)); set("expires_at", Timestamp.from(endsAt))
        }
        if (request.targetLatitude != null || request.targetLongitude != null || request.targetRadiusMeters != null) {
            if (request.targetLatitude == null || request.targetLongitude == null) {
                set("target_location", null)
            } else {
                sets += "target_location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography"
                params["lat"] = request.targetLatitude; params["lng"] = request.targetLongitude
            }
            request.targetRadiusMeters?.let { set("target_radius_m", it) }
        }
        if (sets.isEmpty()) throw BadRequestException("No fields to update")
        val sql = "UPDATE partner_campaigns SET ${sets.joinToString()}, updated_at = now() WHERE id = :id"
        val statement = jdbc.sql(sql).param("id", campaignId)
        params.forEach { (k, v) -> statement.param(k, v) }
        statement.update()
        log.info("campaign updated campaign={} partner={} fields={}", campaignId, partnerId, params.keys)
        return campaign(partnerId, campaignId)
    }

    @Transactional
    fun submit(partnerId: UUID, campaignId: UUID): CampaignV2Dto = transition(partnerId, campaignId, "DRAFT", "IN_REVIEW") {
        val missing = jdbc.sql("SELECT media_asset_id, cta_type, cta_url, service_id FROM partner_campaigns WHERE id = :id").param("id", campaignId)
            .query { rs, _ ->
                buildList {
                    if (rs.getObject("media_asset_id") == null) add("media")
                    if (rs.getString("cta_type") == "LINK" && rs.getString("cta_url") == null) add("ctaUrl")
                    if (rs.getString("cta_type") == "BOOKING" && rs.getObject("service_id") == null) add("serviceId")
                }
            }.single()
        if (missing.isNotEmpty()) throw BadRequestException("Campaign is missing: ${missing.joinToString()}")
    }

    @Transactional
    fun pause(partnerId: UUID, campaignId: UUID): CampaignV2Dto = transition(partnerId, campaignId, "ACTIVE", "PAUSED")

    @Transactional
    fun resume(partnerId: UUID, campaignId: UUID): CampaignV2Dto = transition(partnerId, campaignId, "PAUSED", "ACTIVE")

    @Transactional
    fun end(partnerId: UUID, campaignId: UUID): CampaignV2Dto {
        if (partnerOf(campaignId) != partnerId) throw ForbiddenException("Campaign belongs to another partner")
        val updated = jdbc.sql(
            "UPDATE partner_campaigns SET status = 'ENDED', end_reason = 'ENDED_BY_PARTNER', updated_at = now() WHERE id = :id AND status IN ('DRAFT','IN_REVIEW','ACTIVE','PAUSED')",
        ).param("id", campaignId).update()
        if (updated == 0) throw ConflictException("Campaign already ended")
        log.info("campaign ended campaign={} partner={}", campaignId, partnerId)
        return campaign(partnerId, campaignId)
    }

    private fun transition(partnerId: UUID, campaignId: UUID, from: String, to: String, validate: () -> Unit = {}): CampaignV2Dto {
        if (partnerOf(campaignId) != partnerId) throw ForbiddenException("Campaign belongs to another partner")
        validate()
        val updated = jdbc.sql("UPDATE partner_campaigns SET status = :to, updated_at = now() WHERE id = :id AND status = :from")
            .param("to", to).param("id", campaignId).param("from", from).update()
        if (updated == 0) throw ConflictException("Campaign cannot move from ${from.lowercase()} to ${to.lowercase()} from its current status")
        log.info("campaign transition campaign={} {} -> {}", campaignId, from, to)
        return campaign(partnerId, campaignId)
    }

    // ---- metrics ----

    @Transactional(readOnly = true)
    fun metrics(partnerId: UUID, campaignId: UUID, from: Instant?, to: Instant?): CampaignMetricsDto {
        if (partnerOf(campaignId) != partnerId) throw ForbiddenException("Campaign belongs to another partner")
        val range = (from ?: Instant.EPOCH) to (to ?: Instant.now().plusSeconds(3600))
        val rows = jdbc.sql(
            """
            SELECT placement, type, COUNT(*) AS total FROM campaign_events
            WHERE campaign_id = :id AND occurred_at BETWEEN :from AND :to GROUP BY placement, type
            """,
        ).param("id", campaignId).param("from", Timestamp.from(range.first)).param("to", Timestamp.from(range.second))
            .query { rs, _ -> Triple(rs.getString("placement"), rs.getString("type"), rs.getLong("total")) }.list()
        val bookings = jdbc.sql(
            "SELECT COUNT(*) FROM bookings WHERE campaign_id = :id AND created_at BETWEEN :from AND :to AND status <> 'CANCELLED'",
        ).param("id", campaignId).param("from", Timestamp.from(range.first)).param("to", Timestamp.from(range.second))
            .query(Long::class.java).single()
        val impressions = rows.filter { it.second == "IMPRESSION" }.sumOf { it.third }
        val clicks = rows.filter { it.second == "CLICK" }.sumOf { it.third }
        val charged = jdbc.sql("SELECT credits_charged, budget_credits FROM partner_campaigns WHERE id = :id").param("id", campaignId)
            .query { rs, _ -> rs.getLong(1) to rs.getLong(2) }.single()
        val spent = minOf(charged.first, charged.second.takeIf { it > 0 } ?: charged.first)
        val byPlacement = rows.groupBy({ it.first }, { it.second to it.third }).mapValues { (_, v) -> v.toMap() }
        return CampaignMetricsDto(
            campaignId, range.first, range.second, impressions, clicks, bookings,
            if (impressions > 0) clicks.toDouble() / impressions else 0.0,
            if (impressions > 0) bookings.toDouble() / impressions else 0.0,
            spent,
            if (clicks > 0) spent.toDouble() / clicks else null,
            if (bookings > 0) spent.toDouble() / bookings else null,
            byPlacement,
        )
    }

    // ---- partner profile ----

    @Transactional(readOnly = true)
    fun profile(partnerId: UUID): PartnerProfileDto =
        jdbc.sql("$PROFILE_SELECT WHERE pp.partner_id = :id").param("id", partnerId).query { rs, _ -> profile(rs) }
            .optional().orElseThrow { NotFoundException("Partner profile not found") }

    @Transactional
    fun upsertProfile(partnerId: UUID, request: UpdatePartnerProfileRequest): PartnerProfileDto {
        if ((request.latitude == null) != (request.longitude == null)) throw BadRequestException("latitude and longitude must be provided together")
        jdbc.sql(
            """
            INSERT INTO partner_profiles (partner_id, business_name, category, description, website, phone, address, location,
                                          opening_hours, cover_url, created_at, updated_at)
            VALUES (:id, :name, :category, :description, :website, :phone, :address,
                    CASE WHEN CAST(:lat AS float8) IS NULL THEN NULL ELSE ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography END,
                    :hours, :cover, now(), now())
            ON CONFLICT (partner_id) DO UPDATE SET
                business_name = COALESCE(:name, partner_profiles.business_name),
                category = COALESCE(:category, partner_profiles.category),
                description = COALESCE(:description, partner_profiles.description),
                website = COALESCE(:website, partner_profiles.website),
                phone = COALESCE(:phone, partner_profiles.phone),
                address = COALESCE(:address, partner_profiles.address),
                location = CASE WHEN CAST(:lat AS float8) IS NULL THEN partner_profiles.location
                                ELSE ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)::geography END,
                opening_hours = COALESCE(:hours, partner_profiles.opening_hours),
                cover_url = COALESCE(:cover, partner_profiles.cover_url),
                updated_at = now(), version = partner_profiles.version + 1
            """,
        ).param("id", partnerId).param("name", request.businessName?.trim()?.ifEmpty { null }).param("category", request.category?.name)
            .param("description", request.description?.trim()?.ifEmpty { null }).param("website", request.website?.trim()?.ifEmpty { null })
            .param("phone", request.phone?.trim()?.ifEmpty { null }).param("address", request.address?.trim()?.ifEmpty { null })
            .param("lat", request.latitude).param("lng", request.longitude)
            .param("hours", request.openingHours?.trim()?.ifEmpty { null }).param("cover", request.coverUrl?.trim()?.ifEmpty { null }).update()
        syncPlace(partnerId)
        return profile(partnerId)
    }

    /** Keeps the partner's map place in sync with the business profile (name/location). */
    private fun syncPlace(partnerId: UUID) {
        jdbc.sql(
            """
            UPDATE places pl SET name = pp.business_name, location = pp.location, address = pp.address, updated_at = now()
            FROM partner_profiles pp WHERE pp.partner_id = :id AND pl.partner_id = :id
            """,
        ).param("id", partnerId).update()
        jdbc.sql(
            """
            INSERT INTO places (id, name, category, location, address, description, partner_id, created_at, updated_at)
            SELECT gen_random_uuid(), pp.business_name,
                   CASE pp.category WHEN 'VET' THEN 'VET' WHEN 'CAFE' THEN 'CAFE' WHEN 'HOTEL' THEN 'HOTEL'
                        WHEN 'GROOMING' THEN 'GROOMING' WHEN 'PET_SHOP' THEN 'PET_SHOP' WHEN 'TRAINING' THEN 'TRAINING' ELSE 'OTHER' END,
                   pp.location, pp.address, pp.description, pp.partner_id, now(), now()
            FROM partner_profiles pp WHERE pp.partner_id = :id AND pp.location IS NOT NULL AND NOT EXISTS (SELECT 1 FROM places WHERE partner_id = :id)
            """,
        ).param("id", partnerId).update()
    }

    // ---- admin review ----

    @Transactional(readOnly = true)
    fun reviewQueue(status: String, page: PageRequest): PageResponse<AdminCampaignDto> = PageResponse.of(
        jdbc.sql(
            """
            SELECT c.id, c.partner_id, c.status, c.title, c.description, c.cta, c.cta_type, c.formats, c.starts_at, c.ends_at,
                   c.updated_at, c.reviewed_at, c.rejection_reason,
                   COALESCE(pp.business_name, o.business_name, o.display_name) AS partner_name, COALESCE(pp.verified, FALSE) AS verified
            FROM partner_campaigns c JOIN owners o ON o.id = c.partner_id
            LEFT JOIN partner_profiles pp ON pp.partner_id = c.partner_id
            WHERE c.status = :status ORDER BY c.updated_at LIMIT :limit OFFSET :offset
            """,
        ).param("status", status).param("limit", page.limit).param("offset", page.offset).query { rs, _ -> adminCampaign(rs) }.list(),
        page.page, page.size,
    )

    @Transactional
    fun approve(adminId: UUID, campaignId: UUID): AdminCampaignDto {
        val updated = jdbc.sql(
            "UPDATE partner_campaigns SET status = 'ACTIVE', reviewed_by = :admin, reviewed_at = now(), rejection_reason = NULL, updated_at = now() WHERE id = :id AND status = 'IN_REVIEW'",
        ).param("admin", adminId).param("id", campaignId).update()
        if (updated == 0) throw ConflictException("Campaign is not in review")
        log.info("campaign approved campaign={} by={}", campaignId, adminId)
        return adminCampaign(campaignId)
    }

    /** Rejection sends the campaign back to DRAFT so the partner can fix and resubmit. */
    @Transactional
    fun reject(adminId: UUID, campaignId: UUID, reason: String?): AdminCampaignDto {
        val updated = jdbc.sql(
            "UPDATE partner_campaigns SET status = 'DRAFT', reviewed_by = :admin, reviewed_at = now(), rejection_reason = :reason, updated_at = now() WHERE id = :id AND status = 'IN_REVIEW'",
        ).param("admin", adminId).param("reason", reason?.trim()?.ifEmpty { null } ?: "Rejected by review").param("id", campaignId).update()
        if (updated == 0) throw ConflictException("Campaign is not in review")
        log.info("campaign rejected campaign={} by={}", campaignId, adminId)
        return adminCampaign(campaignId)
    }

    @Transactional
    fun verifyPartner(adminId: UUID, partnerId: UUID, verified: Boolean): PartnerProfileDto {
        val updated = jdbc.sql(
            "UPDATE partner_profiles SET verified = :v, verified_at = CASE WHEN :v THEN now() ELSE NULL END, verified_by = CASE WHEN :v THEN :admin ELSE NULL END, updated_at = now(), version = version + 1 WHERE partner_id = :id",
        ).param("v", verified).param("admin", adminId).param("id", partnerId).update()
        if (updated == 0) throw NotFoundException("Partner profile not found")
        log.info("partner verification partner={} verified={} by={}", partnerId, verified, adminId)
        return profile(partnerId)
    }

    // ---- helpers ----

    private fun statusOf(campaignId: UUID): String =
        jdbc.sql("SELECT status FROM partner_campaigns WHERE id = :id").param("id", campaignId).query(String::class.java)
            .optional().orElseThrow { NotFoundException("Campaign not found") }

    private fun partnerOf(campaignId: UUID): UUID =
        jdbc.sql("SELECT partner_id FROM partner_campaigns WHERE id = :id").param("id", campaignId).query(UUID::class.java)
            .optional().orElseThrow { NotFoundException("Campaign not found") }

    private fun adminCampaign(campaignId: UUID): AdminCampaignDto =
        jdbc.sql(
            """
            SELECT c.id, c.partner_id, c.status, c.title, c.description, c.cta, c.cta_type, c.formats, c.starts_at, c.ends_at,
                   c.updated_at, c.reviewed_at, c.rejection_reason,
                   COALESCE(pp.business_name, o.business_name, o.display_name) AS partner_name, COALESCE(pp.verified, FALSE) AS verified
            FROM partner_campaigns c JOIN owners o ON o.id = c.partner_id
            LEFT JOIN partner_profiles pp ON pp.partner_id = c.partner_id WHERE c.id = :id
            """,
        ).param("id", campaignId).query { rs, _ -> adminCampaign(rs) }.single()

    private fun adminCampaign(rs: ResultSet) = AdminCampaignDto(
        rs.getObject("id", UUID::class.java), rs.getObject("partner_id", UUID::class.java), rs.getString("partner_name"),
        rs.getBoolean("verified"), rs.getString("status"), rs.getString("title"), rs.getString("description"), rs.getString("cta"),
        rs.getString("cta_type"), rs.stringList("formats"), rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at").toInstant(),
        rs.getTimestamp("updated_at").toInstant(), rs.getTimestamp("reviewed_at")?.toInstant(), rs.getString("rejection_reason"),
    )

    private fun campaign(rs: ResultSet) = CampaignV2Dto(
        rs.getObject("id", UUID::class.java), rs.getString("status"), rs.getString("end_reason"), rs.getString("title"),
        rs.getString("description"), rs.getString("target_audience"), rs.getString("cta"), rs.getString("cta_type"), rs.getString("cta_url"),
        rs.getObject("service_id", UUID::class.java), rs.getObject("media_asset_id", UUID::class.java)?.let(MediaUrls::of),
        rs.getLong("budget_credits"), rs.getLong("credits_charged"), rs.getLong("credits_refunded"),
        rs.getTimestamp("starts_at").toInstant(), rs.getTimestamp("ends_at").toInstant(),
        (rs.getObject("lat") as Number?)?.let { lat ->
            TargetLocationDto(lat.toDouble(), (rs.getObject("lng") as Number).toDouble(), (rs.getObject("target_radius_m") as Number?)?.toInt() ?: 10000)
        },
        rs.stringList("target_species"), rs.stringList("target_breeds"), rs.stringList("target_interests"), rs.stringList("formats"),
        rs.getTimestamp("reviewed_at")?.toInstant(), rs.getString("rejection_reason"),
        rs.getTimestamp("created_at").toInstant(), rs.getTimestamp("updated_at").toInstant(),
    )

    private fun profile(rs: ResultSet) = PartnerProfileDto(
        rs.getObject("partner_id", UUID::class.java), rs.getString("business_name"), PartnerCategory.valueOf(rs.getString("category")),
        rs.getString("description"), rs.getString("website"), rs.getString("phone"), rs.getString("address"),
        (rs.getObject("lat") as Number?)?.toDouble(), (rs.getObject("lng") as Number?)?.toDouble(),
        rs.getString("opening_hours"), rs.getString("cover_url"), rs.getBoolean("verified"), rs.getTimestamp("verified_at")?.toInstant(),
        rs.getObject("place_id", UUID::class.java),
    )

    companion object {
        private const val CAMPAIGN_SELECT = """
            SELECT c.id, c.partner_id, c.status, c.end_reason, c.title, c.description, c.target_audience, c.cta, c.cta_type, c.cta_url,
                   c.service_id, c.media_asset_id, c.budget_credits, c.credits_charged, c.credits_refunded,
                   c.starts_at, c.ends_at, c.target_radius_m, c.target_species, c.target_breeds, c.target_interests, c.formats,
                   ST_Y(c.target_location::geometry) AS lat, ST_X(c.target_location::geometry) AS lng,
                   c.reviewed_at, c.rejection_reason, c.created_at, c.updated_at
            FROM partner_campaigns c
        """
        private const val PROFILE_SELECT = """
            SELECT pp.partner_id, pp.business_name, pp.category, pp.description, pp.website, pp.phone, pp.address,
                   ST_Y(pp.location::geometry) AS lat, ST_X(pp.location::geometry) AS lng,
                   pp.opening_hours, pp.cover_url, pp.verified, pp.verified_at, pp.place_id
            FROM partner_profiles pp
        """
    }
}

internal fun ResultSet.stringList(column: String): List<String> =
    (getArray(column)?.array as Array<*>?)?.filterIsInstance<String>().orEmpty()
