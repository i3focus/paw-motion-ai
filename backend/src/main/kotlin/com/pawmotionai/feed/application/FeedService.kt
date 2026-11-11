package com.pawmotionai.feed.application

import com.pawmotionai.feed.adapter.FeedRow
import com.pawmotionai.feed.adapter.JdbcFeedRepository
import com.pawmotionai.feed.adapter.SponsoredRow
import com.pawmotionai.feed.api.AdEventType
import com.pawmotionai.feed.api.AdPlacement
import com.pawmotionai.feed.api.CommentDto
import com.pawmotionai.feed.api.FeedAuthorDto
import com.pawmotionai.feed.api.FeedCardDto
import com.pawmotionai.feed.api.FeedCardType
import com.pawmotionai.feed.api.ReactionSummaryDto
import com.pawmotionai.feed.api.ReactionType
import com.pawmotionai.feed.domain.FeedRanking
import com.pawmotionai.feed.domain.FeedSlot
import com.pawmotionai.feed.domain.RankCandidate
import com.pawmotionai.shared.BadRequestException
import com.pawmotionai.shared.NotFoundException
import com.pawmotionai.shared.PageRequest
import com.pawmotionai.shared.PageResponse
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.time.Clock
import java.time.Instant
import java.util.UUID

@Service
class FeedService(private val repository: JdbcFeedRepository) {

    private val log = LoggerFactory.getLogger(FeedService::class.java)
    internal var clock: Clock = Clock.systemUTC()

    @Transactional(readOnly = true)
    fun feed(viewerId: UUID, page: PageRequest, lat: Double?, lng: Double?): PageResponse<FeedCardDto> {
        validateCoordinates(lat, lng)
        val now = Instant.now(clock)
        val viewer = repository.viewer(viewerId, lat, lng)
        val rows = repository.candidates(viewer, now.minus(FeedRanking.CANDIDATE_WINDOW)).associateBy { it.id }
        val ranked = FeedRanking.rank(
            rows.values.map { RankCandidate(it.id, it.publishedAt, it.reactionCount, it.commentCount, it.followed, it.own) },
            now,
        )
        val window = ranked.drop(page.offset.toInt()).take(page.limit).map { rows.getValue(it.id) }
        val organic = cards(window.take(page.size), viewerId)

        val adSlots = organic.size / FeedRanking.SPONSORED_EVERY
        val ads = if (adSlots == 0) emptyList() else {
            val eligible = repository.sponsoredCandidates(viewer, AdPlacement.FEED_CARD)
                .mapNotNull { row -> FeedRanking.relevance(row.candidate, viewer.profile)?.let { row to it } }
                .sortedWith(compareByDescending<Pair<SponsoredRow, Double>> { it.second }.thenBy { it.first.content.campaignId })
                .map { it.first }
            rotate(eligible, page.page * adSlots).take(adSlots)
        }
        val items = FeedRanking.interleave(organic, ads, { it.content.campaignId }).map { slot ->
            when (slot) {
                is FeedSlot.Organic -> slot.item
                is FeedSlot.Sponsored -> FeedCardDto(
                    id = slot.ad.content.campaignId, type = FeedCardType.SPONSORED, publishedAt = null, author = null,
                    sponsored = slot.ad.content,
                )
            }
        }
        log.debug("feed served viewer={} page={} organic={} sponsored={}", viewerId, page.page, organic.size, items.size - organic.size)
        return PageResponse(items, page.page, page.size, window.size > page.size)
    }

    @Transactional
    fun react(viewerId: UUID, itemId: UUID, type: ReactionType): ReactionSummaryDto {
        requireItem(itemId)
        repository.upsertReaction(itemId, viewerId, type)
        return repository.reactionSummaries(listOf(itemId), viewerId).getValue(itemId)
    }

    @Transactional
    fun unreact(viewerId: UUID, itemId: UUID): ReactionSummaryDto {
        requireItem(itemId)
        repository.deleteReaction(itemId, viewerId)
        return repository.reactionSummaries(listOf(itemId), viewerId).getValue(itemId)
    }

    @Transactional(readOnly = true)
    fun comments(itemId: UUID, page: PageRequest): PageResponse<CommentDto> {
        requireItem(itemId)
        return PageResponse.of(repository.comments(itemId, page), page.page, page.size)
    }

    @Transactional
    fun addComment(viewerId: UUID, itemId: UUID, body: String): CommentDto {
        requireItem(itemId)
        val trimmed = body.trim()
        if (trimmed.isEmpty()) throw BadRequestException("Comment cannot be blank")
        return repository.addComment(itemId, viewerId, trimmed)
    }

    @Transactional
    fun recordAdEvent(viewerId: UUID, campaignId: UUID, type: AdEventType, placement: AdPlacement) {
        if (!repository.campaignServable(campaignId)) throw NotFoundException("Campaign not found")
        val recorded = repository.recordAdEvent(campaignId, type, placement, viewerHash(viewerId))
        log.info("ad event campaign={} type={} placement={} recorded={}", campaignId, type, placement, recorded)
    }

    private fun cards(rows: List<FeedRow>, viewerId: UUID): List<FeedCardDto> {
        val byType = rows.groupBy({ it.type }, { it.refId })
        val activities = repository.activities(byType["ACTIVITY"].orEmpty())
        val posts = repository.posts(byType["POST"].orEmpty())
        val achievements = repository.achievements(byType["ACHIEVEMENT"].orEmpty())
        val reactions = repository.reactionSummaries(rows.map { it.id }, viewerId)
        return rows.mapNotNull { row ->
            val author = FeedAuthorDto(row.ownerId, row.ownerName, row.petId, row.petName, row.petAvatarUrl, row.petSpecies, row.neighborhood)
            val base = FeedCardDto(
                id = row.id, type = FeedCardType.valueOf(row.type), publishedAt = row.publishedAt, author = author,
                reactions = reactions[row.id], commentCount = row.commentCount,
            )
            when (base.type) {
                FeedCardType.ACTIVITY -> activities[row.refId]?.let { base.copy(activity = it) }
                FeedCardType.POST -> posts[row.refId]?.let { base.copy(post = it) }
                FeedCardType.ACHIEVEMENT -> achievements[row.refId]?.let { base.copy(achievement = it) }
                FeedCardType.SPONSORED -> null
            }
        }
    }

    private fun requireItem(itemId: UUID) {
        if (!repository.feedItemExists(itemId)) throw NotFoundException("Feed item not found")
    }

    private fun <T> rotate(list: List<T>, by: Int): List<T> =
        if (list.isEmpty()) list else (list.indices).map { list[(it + by) % list.size] }

    private fun viewerHash(viewerId: UUID): String =
        MessageDigest.getInstance("SHA-256").digest(viewerId.toString().toByteArray()).joinToString("") { "%02x".format(it) }

    companion object {
        fun validateCoordinates(lat: Double?, lng: Double?) {
            if ((lat == null) != (lng == null)) throw BadRequestException("lat and lng must be provided together")
            if (lat != null && lat !in -90.0..90.0) throw BadRequestException("lat must be between -90 and 90")
            if (lng != null && lng !in -180.0..180.0) throw BadRequestException("lng must be between -180 and 180")
        }
    }
}
