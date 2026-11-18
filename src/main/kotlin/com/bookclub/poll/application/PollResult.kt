package com.bookclub.poll.application

data class PollOptionResult(
    val bookId: Long,
    val bookTitle: String,
    val optionIndex: Int,
    val votes: Long
)

data class PollResult(
    val pollRoundId: Long,
    val roundNumber: Int,
    val status: String,
    val winners: List<PollOptionResult>
)