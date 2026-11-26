package com.bookclub.book.application

interface BookMetadataProvider {

    fun search(
        title: String,
        author: String?
    ): List<BookMetadataCandidate>
}