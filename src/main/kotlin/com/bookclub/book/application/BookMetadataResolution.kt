package com.bookclub.book.application

data class BookMetadataResolution(
    val candidate: BookMetadataCandidate,
    val score: Int
)