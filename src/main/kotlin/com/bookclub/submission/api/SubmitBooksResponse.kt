package com.bookclub.submission.api

import java.time.YearMonth

data class SubmitBooksResponse(
    val submissionId: Long,
    val readingMonth: YearMonth,
    val bookCount: Int,
    val message: String
)