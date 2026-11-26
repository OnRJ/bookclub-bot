package com.bookclub.submission.application

import com.bookclub.submission.domain.BookListParser.ParsedBook
import java.time.YearMonth

interface SubmissionWriter {
    fun create(
        telegramChatId: Long,
        telegramUserId: Long,
        username: String?,
        firstName: String,
        readingMonth: YearMonth,
        books: List<ParsedBook>
    ): SubmissionResult
}
