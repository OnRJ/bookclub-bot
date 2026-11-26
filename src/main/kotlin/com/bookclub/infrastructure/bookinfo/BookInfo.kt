package com.bookclub.infrastructure.bookinfo

data class BookInfo(
    val title: String,
    val author: String?,
    val description: String?,
    val pageCount: Int?,
    val coverUrl: String?
)