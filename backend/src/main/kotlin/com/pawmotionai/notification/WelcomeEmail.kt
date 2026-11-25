package com.pawmotionai.notification

import com.pawmotionai.configuration.MailProperties
import com.pawmotionai.shared.LogSafe
import org.slf4j.LoggerFactory
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Component
import org.springframework.stereotype.Service
import java.time.Year

/** Inbound port consumed by other modules (e.g. identity) to welcome newly registered accounts. */
interface SendWelcomeEmailUseCase {

    fun sendTutorWelcome(email: String, displayName: String)

    fun sendPartnerWelcome(email: String, displayName: String, businessName: String)
}

data class RenderedEmail(val subject: String, val html: String, val text: String)

/** Renders the welcome email HTML templates bundled in `resources/templates/email`. */
@Component
class WelcomeEmailTemplates(private val properties: MailProperties) {

    private val tutorTemplate = load(TUTOR_TEMPLATE_PATH)
    private val partnerTemplate = load(PARTNER_TEMPLATE_PATH)

    fun tutorEmail(displayName: String): RenderedEmail {
        val html = render(tutorTemplate, mapOf(
            "displayName" to displayName,
            "appUrl" to properties.appBaseUrl,
            "supportEmail" to properties.supportEmail,
            "year" to Year.now().value.toString()))
        val text = """
            Welcome aboard, $displayName!

            Your Paw Motion AI account is ready. Create your pet's profile, track daily activities,
            get health reminders and book trusted services — all in one place.

            Open Paw Motion AI: ${properties.appBaseUrl}

            Questions? Write to ${properties.supportEmail}.
        """.trimIndent()
        return RenderedEmail("Welcome to Paw Motion AI!", html, text)
    }

    fun partnerEmail(displayName: String, businessName: String): RenderedEmail {
        val html = render(partnerTemplate, mapOf(
            "displayName" to displayName,
            "businessName" to businessName,
            "partnerUrl" to properties.partnerPortalBaseUrl,
            "supportEmail" to properties.supportEmail,
            "year" to Year.now().value.toString()))
        val text = """
            Welcome, $displayName!

            $businessName is now part of the Paw Motion AI partner program. Complete your partner
            profile, purchase credits and launch your first campaign to reach pet tutors near you.

            Go to the partner portal: ${properties.partnerPortalBaseUrl}

            Need help? Write to ${properties.supportEmail}.
        """.trimIndent()
        return RenderedEmail("Welcome to the Paw Motion AI partner program!", html, text)
    }

    private fun render(template: String, values: Map<String, String>): String =
        PLACEHOLDER.replace(template) { match ->
            values[match.groupValues[1]]?.let(::escapeHtml) ?: match.value
        }

    private fun load(path: String): String =
        ClassPathResource(path).inputStream.use { it.readBytes().toString(Charsets.UTF_8) }

    private fun escapeHtml(value: String): String = buildString(value.length) {
        for (char in value) {
            when (char) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&#39;")
                else -> append(char)
            }
        }
    }

    private companion object {
        const val TUTOR_TEMPLATE_PATH = "templates/email/welcome-tutor.html"
        const val PARTNER_TEMPLATE_PATH = "templates/email/welcome-partner.html"
        val PLACEHOLDER = Regex("\\{\\{\\s*([a-zA-Z]+)\\s*}}")
    }
}

/**
 * Sends the rendered welcome email through the configured gateway. Delivery failures are logged
 * but never propagate — a broken mail provider must not break account registration.
 */
@Service
class WelcomeEmailService(
    private val templates: WelcomeEmailTemplates,
    private val sender: EmailSenderPort,
    private val dispatcher: PostCommitDispatcher,
) : SendWelcomeEmailUseCase {

    private val log = LoggerFactory.getLogger(WelcomeEmailService::class.java)

    override fun sendTutorWelcome(email: String, displayName: String) =
        deliver(email, templates.tutorEmail(displayName))

    override fun sendPartnerWelcome(email: String, displayName: String, businessName: String) =
        deliver(email, templates.partnerEmail(displayName, businessName))

    private fun deliver(email: String, rendered: RenderedEmail) {
        // N15: sent only after the registration transaction commits (a rolled-back sign-up sends nothing) and off the
        // request thread. Retries are bounded to MAX_ATTEMPTS inside the task.
        dispatcher.afterCommit {
            var lastFailure: Throwable? = null
            for (attempt in 1..MAX_ATTEMPTS) {
                val result = runCatching { sender.send(OutboundEmail(email, rendered.subject, rendered.html, rendered.text)) }
                if (result.isSuccess) {
                    log.info("welcome email queued to={} id={}", LogSafe.email(email), result.getOrNull())
                    return@afterCommit
                }
                lastFailure = result.exceptionOrNull()
                if (attempt < MAX_ATTEMPTS) Thread.sleep(RETRY_BACKOFF_MS)
            }
            log.warn("welcome email failed to={} reason={}", LogSafe.email(email), lastFailure?.javaClass?.simpleName)
        }
    }

    private companion object {
        const val MAX_ATTEMPTS = 2
        const val RETRY_BACKOFF_MS = 300L
    }
}
