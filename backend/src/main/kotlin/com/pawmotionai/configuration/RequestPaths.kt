package com.pawmotionai.configuration

import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.server.PathContainer
import org.springframework.http.server.RequestPath

/**
 * Canonical request path used to classify routes in the rate-limit filters (C25, C26).
 *
 * Uses Spring's [RequestPath] parser (Spring Framework 7, Spring Boot 4.1): each segment is percent-decoded and matrix
 * parameters (`;k=v`) are dropped, which is what the router sees. Empty and `.` segments are collapsed and `..`
 * segments are resolved, so `//`, `/./` and `/x/../` cannot change the classification.
 */
object RequestPaths {
    fun normalized(request: HttpServletRequest): String =
        normalized(request.requestURI.orEmpty(), request.contextPath.orEmpty())

    internal fun normalized(rawPath: String, contextPath: String = ""): String {
        val segments = try {
            RequestPath.parse(rawPath, contextPath.ifEmpty { null })
                .pathWithinApplication()
                .elements()
                .filterIsInstance<PathContainer.PathSegment>()
                .map { it.valueToMatch() }
        } catch (exception: IllegalArgumentException) {
            // Malformed percent-encoding: fall back to the raw text without matrix parameters.
            rawPath.split("/").map { it.substringBefore(';') }
        }
        val resolved = ArrayDeque<String>()
        for (segment in segments) {
            when (segment) {
                "", "." -> Unit
                ".." -> resolved.removeLastOrNull()
                else -> resolved.addLast(segment)
            }
        }
        return "/" + resolved.joinToString("/")
    }

    /** True when [path] equals [prefix] or lies below it, compared by whole segments. */
    fun hasSegmentPrefix(path: String, prefix: String): Boolean =
        path == prefix || path.startsWith("$prefix/")
}
