package com.bookclub.poll.application

import com.bookclub.poll.domain.PollRoundStatus

data class PollOptionResult(
    val bookId: Long,
    val bookTitle: String,
    val optionIndex: Int,
    val votes: Long
)

data class PollResult(
    val pollRoundId: Long,
    val roundNumber: Int,
    val status: PollRoundStatus,
    val winners: List<PollOptionResult>
)
