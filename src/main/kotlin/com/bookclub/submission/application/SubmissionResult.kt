package com.bookclub.submission.application

import java.time.YearMonth

data class SubmissionResult(
    val submissionId: Long,
    val readingMonth: YearMonth,
    val submittedByTelegramUserId: Long,
    val bookTitles: List<String>
)
