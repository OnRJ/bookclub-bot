package com.bookclub.poll.api

data class BookCatalogResponse(
    val pollRoundId: Long,
    val roundNumber: Int,
    val books: List<CatalogBook>
)

data class CatalogBook(
    val title: String,
    val author: String?,
    val description: String?,
    val pageCount: Int?
)
