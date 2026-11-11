package com.pawmotionai.content

import com.pawmotionai.support.ApiIntegrationTest
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ContentApiIT : ApiIntegrationTest() {

    private val png = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A) + ByteArray(64) { it.toByte() }

    private fun upload(session: Session, bytes: ByteArray, declaredType: String = "image/png"): Response {
        val boundary = "petlife-${UUID.randomUUID()}"
        val body = ByteArrayOutputStream().apply {
            write("--$boundary\r\nContent-Disposition: form-data; name=\"file\"; filename=\"pet.png\"\r\nContent-Type: $declaredType\r\n\r\n".toByteArray())
            write(bytes)
            write("\r\n--$boundary--\r\n".toByteArray())
        }.toByteArray()
        val request = HttpRequest.newBuilder(URI.create("http://localhost:${environment.getProperty("local.server.port")}/api/media"))
            .header("Authorization", "Bearer ${session.token}")
            .header("Content-Type", "multipart/form-data; boundary=$boundary")
            .POST(HttpRequest.BodyPublishers.ofByteArray(body)).build()
        val response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString())
        return Response(response.statusCode(), response.body())
    }

    @Test
    fun `post with uploaded photo appears in followers feed and media is served`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        val bia = registerOwner("Bia")
        val luna = createPet(bia, "Luna")

        val media = expect(201, upload(ana, png))
        assertEquals("image/png", media["contentType"].asString())
        val mediaId = media["id"].asString()
        val download = call("GET", "/api/media/$mediaId")
        assertEquals(200, download.status)

        expect(400, upload(ana, "<html>not an image</html>".toByteArray(), declaredType = "image/png"))
        expect(403, call("POST", "/api/posts", bia.token, mapOf("petId" to luna, "mediaIds" to listOf(mediaId))))
        expect(403, call("POST", "/api/posts", bia.token, mapOf("petId" to thor, "mediaIds" to listOf(mediaId))))
        expect(422, call("POST", "/api/posts", ana.token, mapOf("petId" to thor, "mediaIds" to emptyList<String>())))

        val created = expect(201, call("POST", "/api/posts", ana.token, mapOf(
            "petId" to thor, "caption" to "Beach day", "placeName" to "Ibirapuera", "mediaIds" to listOf(mediaId), "taggedPetIds" to listOf(luna),
        )))
        assertEquals(1, expect(200, call("PUT", "/api/pets/$thor/follow", bia.token))["followers"].asInt())

        val feed = expect(200, call("GET", "/api/feed", bia.token))
        val card = feed["items"].elements().first { it["id"].asString() == created["feedItemId"].asString() }
        assertEquals("POST", card["type"].asString())
        assertEquals("Beach day", card["post"]["caption"].asString())
        assertEquals("/api/media/$mediaId", card["post"]["media"][0]["url"].asString())
        assertEquals(luna.toString(), card["post"]["taggedPetIds"][0].asString())

        expect(403, call("DELETE", "/api/posts/${created["postId"].asString()}", bia.token))
        expect(204, call("DELETE", "/api/posts/${created["postId"].asString()}", ana.token))
        val after = expect(200, call("GET", "/api/feed", bia.token))
        assertTrue(after["items"].elements().none { it["id"].asString() == created["feedItemId"].asString() })
    }

    @Test
    fun `stories are visible to followers and track views`() {
        val ana = registerOwner("Ana")
        val thor = createPet(ana, "Thor")
        val bia = registerOwner("Bia")
        val mediaId = expect(201, upload(ana, png))["id"].asString()

        val story = expect(201, call("POST", "/api/stories", ana.token, mapOf("petId" to thor, "mediaId" to mediaId, "caption" to "Morning!")))
        assertEquals(0, expect(200, call("GET", "/api/stories", bia.token)).size())

        expect(200, call("PUT", "/api/pets/$thor/follow", bia.token))
        val groups = expect(200, call("GET", "/api/stories", bia.token))
        assertEquals(1, groups.size())
        assertEquals(false, groups[0]["allViewed"].asBoolean())

        expect(204, call("POST", "/api/stories/${story["id"].asString()}/view", bia.token))
        assertEquals(true, expect(200, call("GET", "/api/stories", bia.token))[0]["allViewed"].asBoolean())
        expect(404, call("POST", "/api/stories/${UUID.randomUUID()}/view", bia.token))
    }
}
