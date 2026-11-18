package com.bookclub.submission.application

data class SubmitBooksCommand(
    val telegramChatId: Long, val telegramUserId: Long,
    val username: String?, val firstName: String, val bookTitles: List<String>
)
