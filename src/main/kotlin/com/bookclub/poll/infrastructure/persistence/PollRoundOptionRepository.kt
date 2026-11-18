package com.bookclub.poll.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface PollRoundOptionRepository : JpaRepository<PollRoundOptionEntity, Long> {

    fun findAllByPollRoundIdOrderByOptionIndex(
        pollRoundId: Long
    ): List<PollRoundOptionEntity>

    fun findByPollRoundIdAndOptionIndex(
        pollRoundId: Long,
        optionIndex: Int
    ): PollRoundOptionEntity?
}