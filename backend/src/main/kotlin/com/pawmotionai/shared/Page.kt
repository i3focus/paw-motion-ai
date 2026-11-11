package com.pawmotionai.shared

data class PageResponse<T>(val items: List<T>, val page: Int, val size: Int, val hasNext: Boolean) {
    companion object {
        const val MAX_SIZE = 50

        fun <T> of(rowsPlusOne: List<T>, page: Int, size: Int): PageResponse<T> =
            PageResponse(rowsPlusOne.take(size), page, size, rowsPlusOne.size > size)
    }
}

data class PageRequest(val page: Int, val size: Int) {
    val limit: Int get() = size + 1
    val offset: Long get() = page.toLong() * size

    companion object {
        fun of(page: Int?, size: Int?, defaultSize: Int = 20): PageRequest =
            PageRequest((page ?: 0).coerceAtLeast(0), (size ?: defaultSize).coerceIn(1, PageResponse.MAX_SIZE))
    }
}
