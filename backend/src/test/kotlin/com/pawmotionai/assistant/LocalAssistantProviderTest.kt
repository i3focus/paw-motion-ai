package com.pawmotionai.assistant

import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LocalAssistantProviderTest {

    private val provider = LocalAssistantProvider()
    private val pet = UUID.randomUUID()

    private fun ctx(message: String, offers: List<ServiceOffer> = emptyList()) =
        AssistantContext(pet, "Thor", "dog", message, offers, LocalDate.parse("2026-11-11"))

    @Test
    fun `detects intents in English and Portuguese`() {
        assertEquals(AssistantIntent.BOOKING, LocalAssistantProvider.intent("Can you book a bath?"))
        assertEquals(AssistantIntent.BOOKING, LocalAssistantProvider.intent("quero agendar banho e tosa"))
        assertEquals(AssistantIntent.REMINDER, LocalAssistantProvider.intent("Remind me about the vaccine"))
        assertEquals(AssistantIntent.CAPTION, LocalAssistantProvider.intent("write a caption for my photo"))
        assertEquals(AssistantIntent.PLAYDATE, LocalAssistantProvider.intent("set up a playdate"))
        assertEquals(AssistantIntent.GENERAL, LocalAssistantProvider.intent("how much water?"))
    }

    @Test
    fun `sponsored booking proposals are labelled`() {
        val offer = ServiceOffer(UUID.randomUUID(), "Bath", "Happy Spa", 8000, sponsored = true, campaignId = UUID.randomUUID())
        val reply = provider.reply(ctx("book a bath", listOf(offer)))
        assertTrue("(Sponsored)" in reply.text)
        assertEquals(AiActionType.BOOKING, reply.proposal!!.type)
        assertTrue(reply.proposal!!.sponsored)
        assertEquals("2026-11-12T10:00:00Z", reply.proposal!!.payload["startsAt"])
    }

    @Test
    fun `no offers and general questions produce no action`() {
        assertNull(provider.reply(ctx("book a bath")).proposal)
        assertNull(provider.reply(ctx("hello")).proposal)
    }
}
