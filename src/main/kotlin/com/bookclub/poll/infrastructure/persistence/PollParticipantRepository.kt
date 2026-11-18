package com.bookclub.poll.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface PollParticipantRepository :
    JpaRepository<PollParticipantEntity, PollParticipantId> {

    fun countByPollRoundId(pollRoundId: Long): Long

    fun existsByPollRoundIdAndTelegramUserId(
        pollRoundId: Long,
        telegramUserId: Long
    ): Boolean
}