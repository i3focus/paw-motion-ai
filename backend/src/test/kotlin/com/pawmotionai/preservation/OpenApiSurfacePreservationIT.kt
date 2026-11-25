package com.pawmotionai.preservation

import org.junit.jupiter.api.Test
import org.yaml.snakeyaml.Yaml
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Preservation 3.12: the committed `docs/api/openapi.yaml` describes the live API surface (operations, parameters,
 * response codes, schemas and their properties). A remediation PR that changes a contract must regenerate the file.
 *
 * Ordering of properties and the generated `servers` port are volatile between runs, so the comparison is on sets.
 * Read-only: nothing is written to the repository.
 */
class OpenApiSurfacePreservationIT : PreservationApiTest() {

    private fun load(text: String): Map<String, Any?> {
        @Suppress("UNCHECKED_CAST")
        return Yaml().load<Any>(text) as Map<String, Any?>
    }

    @Suppress("UNCHECKED_CAST")
    private fun surface(spec: Map<String, Any?>): Pair<Set<String>, Set<String>> {
        val operations = sortedSetOf<String>()
        (spec["paths"] as Map<String, Map<String, Any?>>).forEach { (path, item) ->
            item.filterKeys { it in setOf("get", "put", "post", "delete", "patch") }.forEach { (method, raw) ->
                val operation = raw as Map<String, Any?>
                val parameters = (operation["parameters"] as? List<Map<String, Any?>>).orEmpty()
                    .map { "${it["in"]}:${it["name"]}" }.sorted()
                val responses = (operation["responses"] as? Map<String, Any?>).orEmpty().keys.map { it.toString() }.sorted()
                operations += "${method.uppercase()} $path params=$parameters responses=$responses"
            }
        }
        val schemas = sortedSetOf<String>()
        val components = spec["components"] as? Map<String, Any?>
        (components?.get("schemas") as? Map<String, Map<String, Any?>>).orEmpty().forEach { (name, schema) ->
            val properties = (schema["properties"] as? Map<String, Any?>).orEmpty().keys.map { it.toString() }.sorted()
            schemas += "$name properties=$properties"
        }
        return operations to schemas
    }

    @Test
    fun `committed openapi yaml matches the live API surface`() {
        val live = HttpClient.newHttpClient().send(
            HttpRequest.newBuilder(URI.create("http://localhost:${environment.getProperty("local.server.port")}/v3/api-docs.yaml")).GET().build(),
            HttpResponse.BodyHandlers.ofString(),
        )
        assertEquals(200, live.statusCode())
        val committed = Files.readString(Path.of("..", "docs", "api", "openapi.yaml"))

        val (liveOperations, liveSchemas) = surface(load(live.body()))
        val (committedOperations, committedSchemas) = surface(load(committed))
        assertTrue(liveOperations.isNotEmpty() && liveSchemas.isNotEmpty())
        assertEquals(emptySet(), liveOperations - committedOperations, "operations in the live API missing from docs/api/openapi.yaml (regenerate with packages/api-client/scripts/generate.sh)")
        assertEquals(emptySet(), committedOperations - liveOperations, "operations in docs/api/openapi.yaml no longer served")
        assertEquals(emptySet(), liveSchemas - committedSchemas, "schemas changed in the live API (regenerate the contract)")
        assertEquals(emptySet(), committedSchemas - liveSchemas, "stale schemas in docs/api/openapi.yaml")
    }
}
