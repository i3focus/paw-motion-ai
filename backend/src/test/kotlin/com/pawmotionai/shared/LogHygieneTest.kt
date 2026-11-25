package com.pawmotionai.shared

import ch.qos.logback.classic.Logger
import ch.qos.logback.classic.spi.ILoggingEvent
import ch.qos.logback.core.read.ListAppender
import com.pawmotionai.configuration.MailProperties
import com.pawmotionai.notification.EmailSendException
import com.pawmotionai.notification.EmailSenderPort
import com.pawmotionai.notification.LoggingEmailSender
import com.pawmotionai.notification.LoggingPushGateway
import com.pawmotionai.notification.OutboundEmail
import com.pawmotionai.notification.PostCommitDispatcher
import com.pawmotionai.notification.WelcomeEmailService
import com.pawmotionai.notification.WelcomeEmailTemplates
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory

class LogHygieneTest {
    private val appender = ListAppender<ILoggingEvent>()
    private val root = LoggerFactory.getLogger(Logger.ROOT_LOGGER_NAME) as Logger

    @BeforeEach fun attach() { appender.start(); root.addAppender(appender) }
    @AfterEach fun detach() { root.detachAppender(appender) }

    private fun logged() = appender.list.joinToString("\n") { it.formattedMessage }

    @Test
    fun `email logs carry a hash and never the address or subject`() {
        val email = "ana.private@example.com"
        LoggingEmailSender().send(OutboundEmail(email, "Segredo do assunto", "<p>x</p>", "x"))
        val dispatcher = object : PostCommitDispatcher() { override fun afterCommit(task: Runnable) = task.run() }
        val ok = EmailSenderPort { "id-1" }
        WelcomeEmailService(WelcomeEmailTemplates(MailProperties()), ok, dispatcher).sendTutorWelcome(email, "Ana")
        val failing = EmailSenderPort { throw EmailSendException("falha para $email") }
        WelcomeEmailService(WelcomeEmailTemplates(MailProperties()), failing, dispatcher).sendTutorWelcome(email, "Ana")
        val text = logged()
        assertTrue(text.contains("email#"))
        assertFalse(text.contains("ana.private"), text)
        assertFalse(text.contains("example.com"), text)
        assertFalse(text.contains("Segredo do assunto"), text)
    }

    @Test
    fun `push stub does not log the private title`() {
        LoggingPushGateway().send(listOf("tok"), "Titulo privado", "corpo privado", emptyMap())
        assertFalse(logged().contains("privado"))
    }

    @Test
    fun `hash is stable and case insensitive`() {
        assertTrue(LogSafe.hash("A@b.com") == LogSafe.hash("a@B.com"))
        assertFalse(LogSafe.email("a@b.com").contains("a@b.com"))
    }
}
