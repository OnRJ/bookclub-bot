package com.bookclub.poll.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface PollEligibleVoterRepository :
    JpaRepository<PollEligibleVoterEntity, PollEligibleVoterId> {

    fun findAllByPollRoundId(pollRoundId: Long): List<PollEligibleVoterEntity>

    fun countByPollRoundId(pollRoundId: Long): Long

    fun existsByPollRoundIdAndTelegramUserId(
        pollRoundId: Long,
        telegramUserId: Long
    ): Boolean
}