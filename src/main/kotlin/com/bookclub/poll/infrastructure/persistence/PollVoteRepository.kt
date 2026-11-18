package com.bookclub.poll.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface PollVoteRepository :
    JpaRepository<PollVoteEntity, PollVoteId> {

    fun deleteAllByPollRoundIdAndTelegramUserId(
        pollRoundId: Long,
        telegramUserId: Long
    )

    fun countByPollRoundIdAndPollOptionId(
        pollRoundId: Long,
        pollOptionId: Long
    ): Long
}