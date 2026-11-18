package com.bookclub.poll.application

data class StartPollCommand(
    val submissionId: Long,
    val telegramChatId: Long
)