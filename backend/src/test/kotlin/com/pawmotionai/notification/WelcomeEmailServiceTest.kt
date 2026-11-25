package com.pawmotionai.notification

import com.pawmotionai.configuration.MailProperties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class WelcomeEmailServiceTest {

    private val sent = mutableListOf<OutboundEmail>()

    private val capturingSender = object : EmailSenderPort {
        override fun send(message: OutboundEmail): String? {
            sent.add(message)
            return "stub-id"
        }
    }

    private val failingSender = EmailSenderPort { throw EmailSendException("provider down") }

    private val inlineDispatcher = object : PostCommitDispatcher() {
        override fun afterCommit(task: Runnable) = task.run()
    }
    private fun service(sender: EmailSenderPort) = WelcomeEmailService(WelcomeEmailTemplates(MailProperties()), sender, inlineDispatcher)

    @Test
    fun sendsRenderedTutorEmailToTheRegisteredAddress() {
        service(capturingSender).sendTutorWelcome("ana@example.com", "Ana")

        assertEquals(1, sent.size)
        val message = sent.single()
        assertEquals("ana@example.com", message.to)
        assertEquals("Welcome to Paw Motion AI!", message.subject)
        assertTrue(message.html.contains("Ana"))
        assertTrue(message.text.contains("Ana"))
    }

    @Test
    fun sendsRenderedPartnerEmailWithBusinessName() {
        service(capturingSender).sendPartnerWelcome("bruno@happypaws.com", "Bruno", "Happy Paws Clinic")

        val message = sent.single()
        assertEquals("bruno@happypaws.com", message.to)
        assertEquals("Welcome to the Paw Motion AI partner program!", message.subject)
        assertTrue(message.html.contains("Happy Paws Clinic"))
        assertTrue(message.text.contains("Happy Paws Clinic"))
    }

    @Test
    fun swallowsProviderFailuresSoRegistrationIsNotBroken() {
        assertDoesNotThrow {
            service(failingSender).sendTutorWelcome("ana@example.com", "Ana")
            service(failingSender).sendPartnerWelcome("bruno@example.com", "Bruno", "Happy Paws Clinic")
        }
    }

    @Test
    fun gatewayFallsBackToTheLoggingStubWhenMailIsDisabled() {
        val resend = ResendEmailSender(MailProperties())
        val stub = LoggingEmailSender()

        assertDoesNotThrow { EmailGateway(resend, stub, MailProperties()).send(
            OutboundEmail("ana@example.com", "Hi", "<p>hi</p>", "hi")) }
    }
}
