package com.pawmotionai.shared

data class PageResponse<T>(val items: List<T>, val page: Int, val size: Int, val hasNext: Boolean) {
    companion object {
        const val MAX_SIZE = 50

        fun <T> of(rowsPlusOne: List<T>, page: Int, size: Int): PageResponse<T> =
            PageResponse(rowsPlusOne.take(size), page, size, rowsPlusOne.size > size)
    }
}

const val DEFAULT_MAX_PAGE = 10_000

class PageOutOfRangeException(maxPage: Int) : RuntimeException("page must be at most $maxPage")

data class PageRequest(val page: Int, val size: Int) {
    val limit: Int get() = size + 1
    val offset: Long get() = page.toLong() * size

    companion object {
        /** Highest accepted `page` (configurable via `app.pagination.max-page`, see PaginationConfiguration). */
        @Volatile
        var maxPage: Int = DEFAULT_MAX_PAGE

        fun of(page: Int?, size: Int?, defaultSize: Int = 20): PageRequest {
            val requested = (page ?: 0).coerceAtLeast(0)
            if (requested > maxPage) {
                throw PageOutOfRangeException(maxPage)
            }
            return PageRequest(requested, (size ?: defaultSize).coerceIn(1, PageResponse.MAX_SIZE))
        }
    }
}
