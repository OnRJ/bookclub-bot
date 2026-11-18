package com.bookclub.submission.api

data class SubmitBooksRequest(
    val initData: String,
    val bookTitles: List<String>
)