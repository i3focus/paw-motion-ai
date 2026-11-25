package com.pawmotionai.shared

import java.net.URI

/** N3: URLs persistidas aceitam apenas http(s) com host nao vazio. */
object UrlValidation {
    fun isHttpUrl(value: String): Boolean {
        val uri = runCatching { URI(value.trim()) }.getOrNull() ?: return false
        val scheme = uri.scheme?.lowercase()
        return (scheme == "http" || scheme == "https") && !uri.host.isNullOrBlank()
    }

    /** Retorna o valor aparado (null se nulo/vazio); lanca 400 se nao for http(s) valida. */
    fun requireHttpUrlOrNull(field: String, value: String?): String? {
        val trimmed = value?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        if (!isHttpUrl(trimmed)) throw BadRequestException("$field must be a valid http:// or https:// URL")
        return trimmed
    }
}
