package com.pawmotionai.configuration

import com.pawmotionai.shared.DEFAULT_MAX_PAGE
import com.pawmotionai.shared.PageRequest
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration

/** Applies `app.pagination.max-page` (N16): requests with a higher `page` get 400 instead of a deep OFFSET scan. */
@Configuration
class PaginationConfiguration(@Value("\${app.pagination.max-page:$DEFAULT_MAX_PAGE}") private val maxPage: Int) {
    @PostConstruct
    fun apply() {
        PageRequest.maxPage = maxPage
    }
}
