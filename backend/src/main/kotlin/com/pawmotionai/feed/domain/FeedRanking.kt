package com.pawmotionai.feed.domain

import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.math.ln
import kotlin.math.max

data class RankCandidate(
    val id: UUID,
    val publishedAt: Instant,
    val reactions: Long,
    val comments: Long,
    val followed: Boolean,
    val own: Boolean,
)

data class ViewerProfile(
    val species: Set<String>,
    val breeds: Set<String>,
    val interests: Set<String>,
)

data class AdCandidate(
    val campaignId: UUID,
    val distanceMeters: Double?,
    val radiusMeters: Int?,
    val targetSpecies: Set<String>,
    val targetBreeds: Set<String>,
    val targetInterests: Set<String>,
)

sealed interface FeedSlot<out T, out A> {
    data class Organic<T>(val item: T) : FeedSlot<T, Nothing>
    data class Sponsored<A>(val ad: A) : FeedSlot<Nothing, A>
}

object FeedRanking {
    const val SPONSORED_EVERY = 5
    val CANDIDATE_WINDOW: Duration = Duration.ofDays(14)

    fun score(candidate: RankCandidate, now: Instant): Double {
        val ageHours = max(0L, Duration.between(candidate.publishedAt, now).toMinutes()) / 60.0
        val recency = 1.0 / (1.0 + ageHours / 12.0)
        val engagement = ln(1.0 + candidate.reactions + 2.0 * candidate.comments) / 5.0
        val followBoost = if (candidate.followed) 0.5 else 0.0
        val ownBoost = if (candidate.own) 0.2 else 0.0
        return recency + engagement + followBoost + ownBoost
    }

    fun rank(candidates: List<RankCandidate>, now: Instant): List<RankCandidate> =
        candidates.sortedWith(
            compareByDescending<RankCandidate> { score(it, now) }
                .thenByDescending { it.publishedAt }
                .thenBy { it.id },
        )

    /** Returns a relevance score, or null when the viewer is outside the campaign's targeting. */
    fun relevance(ad: AdCandidate, viewer: ViewerProfile): Double? {
        val species = viewer.species.map { it.uppercase() }.toSet()
        val breeds = viewer.breeds.map { it.lowercase() }.toSet()
        val interests = viewer.interests.map { it.lowercase() }.toSet()
        if (ad.targetSpecies.isNotEmpty() && ad.targetSpecies.none { it.uppercase() in species }) return null
        if (ad.targetBreeds.isNotEmpty() && ad.targetBreeds.none { it.lowercase() in breeds }) return null
        if (ad.distanceMeters != null && ad.radiusMeters != null && ad.distanceMeters > ad.radiusMeters) return null
        val interestMatches = ad.targetInterests.count { it.lowercase() in interests }
        val proximity = if (ad.distanceMeters == null || ad.radiusMeters == null) 0.0 else 1.0 - ad.distanceMeters / ad.radiusMeters
        val targetingBonus = (if (ad.targetSpecies.isNotEmpty()) 0.5 else 0.0) + (if (ad.targetBreeds.isNotEmpty()) 1.0 else 0.0)
        return 1.0 + interestMatches + proximity + targetingBonus
    }

    /**
     * Inserts at most one sponsored item after every [every] organic items. Sponsored items are never first,
     * never consecutive, and a campaign appears at most once per page.
     */
    fun <T, A> interleave(organic: List<T>, ads: List<A>, campaignOf: (A) -> UUID, every: Int = SPONSORED_EVERY): List<FeedSlot<T, A>> {
        val queue = ads.distinctBy(campaignOf).iterator()
        val result = ArrayList<FeedSlot<T, A>>(organic.size + organic.size / every)
        organic.forEachIndexed { index, item ->
            result += FeedSlot.Organic(item)
            if ((index + 1) % every == 0 && queue.hasNext()) result += FeedSlot.Sponsored(queue.next())
        }
        return result
    }
}
