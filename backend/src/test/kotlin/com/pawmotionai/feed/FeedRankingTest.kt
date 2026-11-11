package com.pawmotionai.feed

import com.pawmotionai.feed.domain.AdCandidate
import com.pawmotionai.feed.domain.FeedRanking
import com.pawmotionai.feed.domain.FeedSlot
import com.pawmotionai.feed.domain.RankCandidate
import com.pawmotionai.feed.domain.ViewerProfile
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FeedRankingTest {

    private val now = Instant.parse("2026-11-11T12:00:00Z")

    private fun candidate(hoursAgo: Long, reactions: Long = 0, followed: Boolean = false) =
        RankCandidate(UUID.randomUUID(), now.minus(Duration.ofHours(hoursAgo)), reactions, 0, followed, own = false)

    @Test
    fun `newer items rank above older items with equal engagement`() {
        val old = candidate(48)
        val fresh = candidate(1)
        assertEquals(listOf(fresh, old), FeedRanking.rank(listOf(old, fresh), now))
    }

    @Test
    fun `followed pets and engagement boost ranking`() {
        val stranger = candidate(2)
        val followed = candidate(6, followed = true)
        val popular = candidate(3, reactions = 200)
        val ranked = FeedRanking.rank(listOf(stranger, followed, popular), now)
        assertEquals(stranger, ranked.last())
    }

    @Test
    fun `sponsored items are never first, never consecutive and never repeat a campaign`() {
        val organic = (1..17).toList()
        val campaign = UUID.randomUUID()
        val ads = listOf(campaign, campaign, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID())

        val slots = FeedRanking.interleave(organic, ads, { it })

        assertTrue(slots.first() is FeedSlot.Organic)
        slots.zipWithNext().forEach { (a, b) -> assertTrue(a is FeedSlot.Organic || b is FeedSlot.Organic) }
        val sponsored = slots.filterIsInstance<FeedSlot.Sponsored<UUID>>().map { it.ad }
        assertEquals(3, sponsored.size)
        assertEquals(sponsored.distinct(), sponsored)
        assertEquals(5, slots.indexOfFirst { it is FeedSlot.Sponsored })
        assertTrue(organic.size / sponsored.size >= FeedRanking.SPONSORED_EVERY)
    }

    @Test
    fun `short pages carry no sponsored items`() {
        assertTrue(FeedRanking.interleave(listOf(1, 2, 3, 4), listOf(UUID.randomUUID()), { it }).all { it is FeedSlot.Organic })
    }

    @Test
    fun `ad targeting filters species, breed and radius`() {
        val viewer = ViewerProfile(setOf("DOG"), setOf("Golden Retriever"), setOf("swimming"))
        fun ad(species: Set<String> = emptySet(), breeds: Set<String> = emptySet(), distance: Double? = null, radius: Int? = null) =
            AdCandidate(UUID.randomUUID(), distance, radius, species, breeds, setOf("swimming"))

        assertNull(FeedRanking.relevance(ad(species = setOf("CAT")), viewer))
        assertNull(FeedRanking.relevance(ad(breeds = setOf("poodle")), viewer))
        assertNull(FeedRanking.relevance(ad(distance = 12_000.0, radius = 10_000), viewer))
        val near = assertNotNull(FeedRanking.relevance(ad(species = setOf("dog"), distance = 1_000.0, radius = 10_000), viewer))
        val far = assertNotNull(FeedRanking.relevance(ad(species = setOf("dog"), distance = 9_000.0, radius = 10_000), viewer))
        assertTrue(near > far)
    }
}
