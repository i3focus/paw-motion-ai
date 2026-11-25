package com.pawmotionai.notification

import com.pawmotionai.configuration.MailProperties
import com.sun.net.httpserver.HttpServer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.ObjectMapper
import java.net.InetSocketAddress
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** N15: explicit timeouts and bounded retries, against a local fake provider (never a real service). */
class ExternalIoTimeoutTest {
    private val hits = AtomicInteger()
    private var server: HttpServer? = null

    private fun start(handler: (Int) -> Pair<Int, Long>): String {
        val s = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        s.createContext("/") { ex ->
            val (code, delayMs) = handler(hits.incrementAndGet())
            ex.requestBody.readAllBytes()
            Thread.sleep(delayMs)
            runCatching {
                val body = """{"id":"x"}""".toByteArray()
                ex.sendResponseHeaders(code, body.size.toLong())
                ex.responseBody.use { it.write(body) }
            }
        }
        s.executor = java.util.concurrent.Executors.newCachedThreadPool()
        s.start()
        server = s
        return "http://127.0.0.1:${s.address.port}"
    }

    @AfterEach fun stop() { server?.stop(0) }

    private fun push(url: String, attempts: Int) = ExpoPushGateway(ObjectMapper(), true, url, 500, 700, attempts)

    @Test
    fun `slow push provider is cut at the request timeout with bounded attempts`() {
        val url = start { 200 to 5000L }
        val started = System.nanoTime()
        push(url, 2).send(listOf("t"), "a", "b", emptyMap())
        val elapsedMs = (System.nanoTime() - started) / 1_000_000
        assertTrue(elapsedMs < 3000, "took ${elapsedMs}ms")
        assertEquals(2, hits.get())
    }

    @Test
    fun `push retries a 503 once and stops after success`() {
        val url = start { n -> (if (n == 1) 503 else 200) to 0L }
        push(url, 3).send(listOf("t"), "a", "b", emptyMap())
        assertEquals(2, hits.get())
    }

    @Test
    fun `push does not retry a 4xx`() {
        val url = start { 400 to 0L }
        push(url, 3).send(listOf("t"), "a", "b", emptyMap())
        assertEquals(1, hits.get())
    }

    @Test
    fun `slow email provider fails at the read timeout`() {
        val url = start { 200 to 5000L }
        val props = MailProperties().apply {
            resend.baseUrl = url; resend.apiKey = "k"; resend.connectTimeoutMs = 500; resend.readTimeoutMs = 700
        }
        val started = System.nanoTime()
        assertThrows<EmailSendException> { ResendEmailSender(props).send(OutboundEmail("a@b.c", "s", "<p>h</p>", "h")) }
        assertTrue((System.nanoTime() - started) / 1_000_000 < 3000)
    }
}
