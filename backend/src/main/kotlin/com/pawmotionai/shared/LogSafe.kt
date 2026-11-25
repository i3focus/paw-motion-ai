package com.pawmotionai.shared

import java.security.MessageDigest

/** Helpers to keep PII (full emails, private text) out of logs. */
object LogSafe {
    /** Stable short SHA-256 fingerprint, useful to correlate log lines without exposing the value. */
    fun hash(value: String?): String =
        if (value == null) "none" else MessageDigest.getInstance("SHA-256").digest(value.lowercase().toByteArray())
            .take(4).joinToString("") { "%02x".format(it) }

    /** `ana@example.com` -> `a***@e***.com`-style hash tag; never contains the local part or domain. */
    fun email(value: String?): String = "email#" + hash(value)
}
