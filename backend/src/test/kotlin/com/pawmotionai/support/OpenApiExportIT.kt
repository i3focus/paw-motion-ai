package com.pawmotionai.support

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertTrue

/**
 * Regenerates docs/api/openapi.yaml from the live application. Run with:
 * `EXPORT_OPENAPI=true ./gradlew test --tests 'OpenApiExportIT'`
 */
class OpenApiExportIT : ApiIntegrationTest() {

    @Test
    @EnabledIfEnvironmentVariable(named = "EXPORT_OPENAPI", matches = "true")
    fun `export openapi yaml`() {
        val port = environment.getProperty("local.server.port")
        val response = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://localhost:$port/v3/api-docs.yaml")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertTrue(response.statusCode() == 200 && response.body().contains("openapi:"),
            "status=${response.statusCode()} body=${response.body().take(400)}")
        val target = Path.of("..", "docs", "api", "openapi.yaml")
        Files.createDirectories(target.parent)
        Files.writeString(target, response.body())
        println("exported ${target.toAbsolutePath()} (${response.body().length} chars)")
    }
}
