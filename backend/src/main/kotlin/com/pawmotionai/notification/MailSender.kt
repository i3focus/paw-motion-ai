package com.pawmotionai.notification

import com.pawmotionai.configuration.MailProperties
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Primary
import org.springframework.http.MediaType
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.client.RestClientException
import org.springframework.web.client.RestClientResponseException
import tools.jackson.databind.JsonNode
import java.net.http.HttpClient
import java.time.Duration

/** Outbound email ready to be handed to a provider. Plain text is required alongside HTML. */
data class OutboundEmail(
    val to: String,
    val subject: String,
    val html: String,
    val text: String,
)

class EmailSendException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Outbound email gateway. The Resend adapter is only used when `app.mail.enabled=true` and an API key is set. */
fun interface EmailSenderPort {
    /** Returns the provider message id on success, or null when the sender does not report one. */
    fun send(message: OutboundEmail): String?
}

@Component
class LoggingEmailSender : EmailSenderPort {
    private val log = LoggerFactory.getLogger(LoggingEmailSender::class.java)

    override fun send(message: OutboundEmail): String? {
        log.info("email stub to={} subject={}", message.to, message.subject)
        return null
    }
}

@Component
class ResendEmailSender(private val properties: MailProperties) : EmailSenderPort {

    private val log = LoggerFactory.getLogger(ResendEmailSender::class.java)
    // Explicit connect/read timeouts (N15): a stalled provider can never hold a worker thread indefinitely.
    private val restClient: RestClient = RestClient.builder().baseUrl(properties.resend.baseUrl)
        .requestFactory(JdkClientHttpRequestFactory(
            HttpClient.newBuilder().connectTimeout(Duration.ofMillis(properties.resend.connectTimeoutMs)).build())
            .apply { setReadTimeout(Duration.ofMillis(properties.resend.readTimeoutMs)) })
        .build()

    override fun send(message: OutboundEmail): String? {
        val body = mapOf(
            "from" to properties.from,
            "to" to listOf(message.to),
            "subject" to message.subject,
            "html" to message.html,
            "text" to message.text,
        )
        try {
            val response = restClient.post()
                .uri("/emails")
                .header("Authorization", "Bearer ${properties.resend.apiKey}")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode::class.java)
            val id = response?.path("id")?.asString(null)
            log.info("resend email sent to={} id={}", message.to, id)
            return id
        } catch (exception: RestClientResponseException) {
            throw EmailSendException(
                "Resend send failed status=${exception.statusCode} body=${exception.responseBodyAsString.take(300)}",
                exception)
        } catch (exception: RestClientException) {
            throw EmailSendException("Resend send failed: ${exception.message}", exception)
        }
    }
}

/** Chooses the Resend gateway when enabled and configured, otherwise the logging stub. */
@Component
@Primary
class EmailGateway(resend: ResendEmailSender, stub: LoggingEmailSender, properties: MailProperties) : EmailSenderPort {
    private val delegate = if (properties.enabled && properties.resend.apiKey.isNotBlank()) resend else stub
    override fun send(message: OutboundEmail): String? = delegate.send(message)
}
