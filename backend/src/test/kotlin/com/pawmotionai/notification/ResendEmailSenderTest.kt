package com.pawmotionai.notification

import com.pawmotionai.configuration.MailProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress

class ResendEmailSenderTest {

    private val mapper = ObjectMapper()
    private var capturedBody = ""
    private var capturedAuth: String? = null
    private var responseStatus = 200
    private var responseBody = "{\"id\":\"msg_123\"}"

    private val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0).apply {
        createContext("/emails") { exchange ->
            capturedAuth = exchange.requestHeaders.getFirst("Authorization")
            capturedBody = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
            val bytes = responseBody.toByteArray(Charsets.UTF_8)
            exchange.responseHeaders.add("Content-Type", "application/json")
            exchange.sendResponseHeaders(responseStatus, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
        start()
    }

    private val properties = MailProperties().apply {
        from = "Paw Motion AI <welcome@i3pet.com.br>"
        resend.apiKey = "re_test_key"
        resend.baseUrl = "http://127.0.0.1:${server.address.port}"
    }
    private val sender = ResendEmailSender(properties)

    @AfterEach
    fun tearDown() = server.stop(0)

    @Test
    fun postsEmailToResendApiAndReturnsProviderId() {
        val id = sender.send(OutboundEmail("ana@example.com", "Welcome to Paw Motion AI!", "<p>hi</p>", "hi"))

        assertEquals("msg_123", id)
        assertEquals("Bearer re_test_key", capturedAuth)
        val payload = mapper.readTree(capturedBody)
        assertEquals("Paw Motion AI <welcome@i3pet.com.br>", payload.path("from").asString())
        assertEquals("ana@example.com", payload.path("to").path(0).asString())
        assertEquals("Welcome to Paw Motion AI!", payload.path("subject").asString())
        assertEquals("<p>hi</p>", payload.path("html").asString())
        assertEquals("hi", payload.path("text").asString())
    }

    @Test
    fun wrapsResendErrorsInEmailSendException() {
        responseStatus = 422
        responseBody = "{\"message\":\"invalid to\"}"

        val exception = assertThrows(EmailSendException::class.java) {
            sender.send(OutboundEmail("bad@example.com", "Hi", "<p>hi</p>", "hi"))
        }
        assertTrue(exception.message!!.contains("422") || exception.message!!.contains("UNPROCESSABLE"))
    }
}
