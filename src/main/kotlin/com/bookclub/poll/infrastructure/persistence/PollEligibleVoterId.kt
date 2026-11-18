package com.bookclub.poll.infrastructure.persistence

import java.io.Serializable

data class PollEligibleVoterId(
    var pollRoundId: Long = 0,
    var telegramUserId: Long = 0
) : Serializable