package com.pawmotionai.configuration

/**
 * Settings of the new edge buckets (`app.rate-limit.*`). In [Mode.SHADOW] a bucket measures and records violations
 * (`ratelimit.shadow_violations`) without answering 429. Pre-existing limits (10 auth attempts/min/IP/route,
 * 120 writes/min, 30 AI messages/min) stay enforced in both modes (preservation 3.7).
 */
data class RateLimitSettings(
    val mode: Mode = Mode.SHADOW,
    val webhookPerMinute: Int = 600,
) {
    enum class Mode {
        SHADOW, ENFORCE;

        companion object {
            fun parse(value: String?): Mode =
                if (value.isNullOrBlank()) SHADOW else valueOf(value.trim().uppercase())
        }
    }
}
