package com.pawmotionai.assistant

import com.pawmotionai.booking.BookingService
import com.pawmotionai.booking.BookingSource
import com.pawmotionai.booking.CreateBookingRequest
import com.pawmotionai.health.HealthRecordRequest
import com.pawmotionai.health.HealthRecordType
import com.pawmotionai.health.HealthService
import com.pawmotionai.map.CreatePlaydateRequest
import com.pawmotionai.map.MapService
import com.pawmotionai.map.PlaydateMode
import com.pawmotionai.shared.ConflictException
import com.pawmotionai.shared.ForbiddenException
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import com.pawmotionai.shared.PetAccess
import com.pawmotionai.shared.QuotaExceededException
import org.slf4j.LoggerFactory
import org.springframework.jdbc.core.simple.JdbcClient
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import tools.jackson.databind.ObjectMapper
import java.security.MessageDigest
import java.sql.ResultSet
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.util.UUID

@Service
class AssistantService(
    private val jdbc: JdbcClient,
    private val provider: AssistantProviderPort,
    private val petAccess: PetAccess,
    private val mapper: ObjectMapper,
    private val bookings: BookingService,
    private val health: HealthService,
    private val map: MapService,
) {
    private val log = LoggerFactory.getLogger(AssistantService::class.java)

    companion object {
        const val FREE_MONTHLY_MESSAGES = 10
    }

    @Transactional(readOnly = true)
    fun conversations(ownerId: UUID, page: PageRequest): PageResponse<ConversationDto> = PageResponse.of(
        jdbc.sql("SELECT id, pet_id, title, updated_at FROM ai_conversations WHERE owner_id = :owner ORDER BY updated_at DESC, id LIMIT :limit OFFSET :offset")
            .param("owner", ownerId).param("limit", page.limit).param("offset", page.offset)
            .query { rs, _ -> ConversationDto(rs.getObject(1, UUID::class.java), rs.getObject(2, UUID::class.java), rs.getString(3), rs.getTimestamp(4).toInstant()) }
            .list(),
        page.page, page.size,
    )

    @Transactional
    fun createConversation(ownerId: UUID, petId: UUID?): ConversationDto {
        petId?.let { petAccess.requireOwnedActivePet(ownerId, it) }
        val id = UUID.randomUUID()
        jdbc.sql("INSERT INTO ai_conversations (id, owner_id, pet_id, title, created_at, updated_at) VALUES (:id, :owner, :pet, 'New conversation', now(), now())")
            .param("id", id).param("owner", ownerId).param("pet", petId).update()
        return conversations(ownerId, PageRequest.of(0, 50)).items.first { it.id == id }
    }

    @Transactional(readOnly = true)
    fun messages(ownerId: UUID, conversationId: UUID): List<AiMessageDto> {
        requireConversation(ownerId, conversationId)
        val actions = jdbc.sql("SELECT * FROM ai_actions WHERE conversation_id = :id").param("id", conversationId)
            .query { rs, _ -> rs.getObject("message_id", UUID::class.java) to action(rs) }.list().toMap()
        return jdbc.sql("SELECT id, role, content, created_at FROM ai_messages WHERE conversation_id = :id ORDER BY created_at, id")
            .param("id", conversationId).query { rs, _ ->
                val id = rs.getObject(1, UUID::class.java)
                AiMessageDto(id, rs.getString(2), rs.getString(3), rs.getTimestamp(4).toInstant(), actions[id])
            }.list()
    }

    @Transactional(readOnly = true)
    fun usage(ownerId: UUID): AiUsageDto {
        val plan = jdbc.sql("SELECT plan FROM owners WHERE id = :id").param("id", ownerId).query(String::class.java).single()
        val used = jdbc.sql("SELECT COALESCE(MAX(messages), 0) FROM ai_usage WHERE owner_id = :id AND period = :period")
            .param("id", ownerId).param("period", YearMonth.now(ZoneOffset.UTC).toString()).query(Int::class.java).single()
        return AiUsageDto(plan, used, if (plan == "PREMIUM") null else FREE_MONTHLY_MESSAGES)
    }

    @Transactional
    fun send(ownerId: UUID, conversationId: UUID, content: String): SendMessageResponse {
        val petId = requireConversation(ownerId, conversationId)
        consumeQuota(ownerId)
        val text = content.trim()
        val userMessageId = insertMessage(conversationId, "USER", text, null, null)
        data class Pet(val name: String, val species: String)
        val pet = petId?.let {
            jdbc.sql("SELECT name, lower(species) FROM pets WHERE id = :id").param("id", it).query { rs, _ -> Pet(rs.getString(1), rs.getString(2)) }.single()
        }
        val offers = if (LocalAssistantProvider.intent(text) == AssistantIntent.BOOKING) offers(ownerId, pet?.species) else emptyList()
        val reply = provider.reply(AssistantContext(petId, pet?.name, pet?.species, text, offers))
        val assistantMessageId = insertMessage(conversationId, "ASSISTANT", reply.text, reply.provider, reply.model)
        reply.proposal?.let { proposal ->
            jdbc.sql(
                """
                INSERT INTO ai_actions (id, conversation_id, message_id, owner_id, type, status, payload, sponsored, campaign_id, created_at, updated_at)
                VALUES (:id, :conversation, :message, :owner, :type, 'PROPOSED', CAST(:payload AS jsonb), :sponsored, :campaign, now(), now())
                """,
            ).param("id", UUID.randomUUID()).param("conversation", conversationId).param("message", assistantMessageId).param("owner", ownerId)
                .param("type", proposal.type.name).param("payload", mapper.writeValueAsString(proposal.payload))
                .param("sponsored", proposal.sponsored).param("campaign", proposal.campaignId).update()
            proposal.campaignId?.let { recordImpression(it, ownerId) }
        }
        jdbc.sql(
            "UPDATE ai_conversations SET updated_at = now(), title = CASE WHEN title = 'New conversation' THEN LEFT(:title, 120) ELSE title END WHERE id = :id",
        ).param("title", text).param("id", conversationId).update()
        log.info("assistant reply conversation={} provider={} proposal={}", conversationId, reply.provider, reply.proposal?.type)
        val all = messages(ownerId, conversationId)
        return SendMessageResponse(all.first { it.id == userMessageId }, all.first { it.id == assistantMessageId }, usage(ownerId))
    }

    @Transactional
    fun confirm(ownerId: UUID, actionId: UUID): AiActionDto {
        val action = lockProposed(ownerId, actionId)
        val payload = action.payload
        val petId = (payload["petId"] as String?)?.let(UUID::fromString)
        val resultRef: UUID? = when (action.type) {
            AiActionType.BOOKING -> bookings.book(
                ownerId,
                CreateBookingRequest(UUID.fromString(payload["serviceId"] as String), petId, Instant.parse(payload["startsAt"] as String), null, action.campaignId),
                BookingSource.AI, actionId,
            ).id
            AiActionType.REMINDER -> health.addRecord(
                ownerId, petId!!,
                HealthRecordRequest(HealthRecordType.valueOf(payload["type"] as String), payload["name"] as String, null,
                    LocalDate.parse(payload["dueOn"] as String), null, null, "Created by the PETLIFE assistant"),
            ).id
            AiActionType.PLAYDATE -> map.createPlaydate(
                ownerId, CreatePlaydateRequest(petId, PlaydateMode.NOW, null, null, null, null, null, payload["message"] as String?),
            ).id
            AiActionType.CAPTION -> null
        }
        jdbc.sql("UPDATE ai_actions SET status = 'EXECUTED', result_ref = :ref, updated_at = now(), version = version + 1 WHERE id = :id")
            .param("ref", resultRef).param("id", actionId).update()
        log.info("assistant action executed action={} type={} result={}", actionId, action.type, resultRef)
        return action(actionId)
    }

    @Transactional
    fun cancel(ownerId: UUID, actionId: UUID): AiActionDto {
        lockProposed(ownerId, actionId)
        jdbc.sql("UPDATE ai_actions SET status = 'CANCELLED', updated_at = now(), version = version + 1 WHERE id = :id").param("id", actionId).update()
        return action(actionId)
    }

    private fun offers(ownerId: UUID, species: String?): List<ServiceOffer> = jdbc.sql(
        """
        (SELECT s.id, s.name, pp.business_name, s.price_cents, TRUE AS sponsored, c.id AS campaign_id, 0 AS rank
         FROM partner_campaigns c
         JOIN partner_services s ON s.id = c.service_id AND s.active
         JOIN partner_profiles pp ON pp.partner_id = c.partner_id
         WHERE c.status = 'ACTIVE' AND c.starts_at <= now() AND c.ends_at > now() AND c.cta_type = 'BOOKING'
           AND 'AI_RECOMMENDATION' = ANY(c.formats) AND c.partner_id <> :owner
           AND (cardinality(c.target_species) = 0 OR upper(CAST(:species AS text)) = ANY(c.target_species))
         ORDER BY c.id LIMIT 3)
        UNION ALL
        (SELECT s.id, s.name, pp.business_name, s.price_cents, FALSE, NULL, 1
         FROM partner_services s JOIN partner_profiles pp ON pp.partner_id = s.partner_id
         WHERE s.active AND s.partner_id <> :owner
         ORDER BY pp.verified DESC, s.price_cents, s.id LIMIT 3)
        ORDER BY rank
        """,
    ).param("owner", ownerId).param("species", species).query { rs, _ ->
        ServiceOffer(rs.getObject(1, UUID::class.java), rs.getString(2), rs.getString(3), rs.getLong(4), rs.getBoolean(5), rs.getObject(6, UUID::class.java))
    }.list()

    private fun consumeQuota(ownerId: UUID) {
        val usage = usage(ownerId)
        if (usage.limit != null && usage.used >= usage.limit) {
            throw QuotaExceededException("Free plan includes ${usage.limit} assistant messages per month. Upgrade to Premium for unlimited messages.")
        }
        jdbc.sql(
            "INSERT INTO ai_usage (owner_id, period, messages) VALUES (:owner, :period, 1) ON CONFLICT (owner_id, period) DO UPDATE SET messages = ai_usage.messages + 1",
        ).param("owner", ownerId).param("period", YearMonth.now(ZoneOffset.UTC).toString()).update()
    }

    private fun recordImpression(campaignId: UUID, ownerId: UUID) {
        val viewer = MessageDigest.getInstance("SHA-256").digest(ownerId.toString().toByteArray()).joinToString("") { "%02x".format(it) }
        jdbc.sql("INSERT INTO campaign_events (id, campaign_id, type, placement, viewer_hash, occurred_at) VALUES (:id, :campaign, 'IMPRESSION', 'AI_RECOMMENDATION', :viewer, now())")
            .param("id", UUID.randomUUID()).param("campaign", campaignId).param("viewer", viewer).update()
    }

    private fun insertMessage(conversationId: UUID, role: String, content: String, provider: String?, model: String?): UUID {
        val id = UUID.randomUUID()
        jdbc.sql(
            "INSERT INTO ai_messages (id, conversation_id, role, content, provider, model, created_at) VALUES (:id, :conversation, :role, :content, :provider, :model, clock_timestamp())",
        ).param("id", id).param("conversation", conversationId).param("role", role).param("content", content.take(4000))
            .param("provider", provider).param("model", model).update()
        return id
    }

    private fun requireConversation(ownerId: UUID, conversationId: UUID): UUID? {
        data class Conv(val owner: UUID, val pet: UUID?)
        val conv = jdbc.sql("SELECT owner_id, pet_id FROM ai_conversations WHERE id = :id").param("id", conversationId)
            .query { rs, _ -> Conv(rs.getObject(1, UUID::class.java), rs.getObject(2, UUID::class.java)) }
            .optional().orElseThrow { NotFoundException("Conversation not found") }
        if (conv.owner != ownerId) throw NotFoundException("Conversation not found")
        return conv.pet
    }

    private fun lockProposed(ownerId: UUID, actionId: UUID): AiActionDto {
        val action = jdbc.sql("SELECT * FROM ai_actions WHERE id = :id FOR UPDATE").param("id", actionId).query { rs, _ -> rs.getObject("owner_id", UUID::class.java) to action(rs) }
            .optional().orElseThrow { NotFoundException("Action not found") }
        if (action.first != ownerId) throw ForbiddenException("Not your action")
        if (action.second.status != AiActionStatus.PROPOSED) throw ConflictException("Action was already ${action.second.status.name.lowercase()}")
        return action.second
    }

    private fun action(id: UUID): AiActionDto =
        jdbc.sql("SELECT * FROM ai_actions WHERE id = :id").param("id", id).query { rs, _ -> action(rs) }.single()

    @Suppress("UNCHECKED_CAST")
    private fun action(rs: ResultSet) = AiActionDto(
        rs.getObject("id", UUID::class.java), AiActionType.valueOf(rs.getString("type")), AiActionStatus.valueOf(rs.getString("status")),
        mapper.readValue(rs.getString("payload"), Map::class.java) as Map<String, Any?>, rs.getBoolean("sponsored"),
        rs.getObject("campaign_id", UUID::class.java), rs.getObject("result_ref", UUID::class.java), rs.getString("error"),
    )
}
