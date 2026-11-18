package com.bookclub.submission.application

import java.time.YearMonth
import java.time.ZoneId

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

interface ClubLookup {
    fun findTimezoneByTelegramChatId(telegramChatId: Long): ZoneId?
}
