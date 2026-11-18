package com.bookclub.poll.application

data class PollRoundResult(
    val pollRoundId: Long,
    val submissionId: Long,
    val roundNumber: Int,
    val telegramChatId: Long,
    val bookTitles: List<String>,
    val eligibleVotersCount: Int
)