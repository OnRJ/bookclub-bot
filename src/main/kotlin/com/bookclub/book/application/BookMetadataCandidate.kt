package com.bookclub.book.application

data class BookMetadataCandidate(
    val title: String,
    val author: String?,
    val description: String?,
    val pageCount: Int?,
    val isbn: String?,
    val externalId: String?,
    val source: String
)
