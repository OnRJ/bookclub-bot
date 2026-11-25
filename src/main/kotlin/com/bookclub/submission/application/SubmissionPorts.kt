package com.bookclub.submission.application

import java.time.YearMonth

interface SubmissionWriter {
    fun create(
        telegramChatId: Long,
        telegramUserId: Long,
        username: String?,
        firstName: String,
        readingMonth: YearMonth,
        bookTitles: List<String>
    ): SubmissionResult
}
