package com.bookclub.book.application

data class BookDescriptionGenerationResult(
    val description: String?,
    val success: Boolean,
    val reason: String?,
    val author: String? = null,
    val pageCount: Int? = null,
    val coverUrl: String? = null
)
