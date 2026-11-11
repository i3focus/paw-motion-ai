package com.pawmotionai.shared

import java.util.UUID

object MediaUrls {
    fun of(mediaId: UUID): String = "/api/media/$mediaId"
}
