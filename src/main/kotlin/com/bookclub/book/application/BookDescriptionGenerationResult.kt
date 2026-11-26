package com.bookclub.book.application

data class BookDescriptionGenerationResult(
    val description: String?,
    val success: Boolean,
    val reason: String?
)
