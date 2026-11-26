package com.bookclub.book.application

data class BookDescriptionRequest(
    val title: String,
    val author: String?,
    val sourceDescription: String?,
    val pageCount: Int?,
    val isbn: String?,
    val metadataSource: String
)
