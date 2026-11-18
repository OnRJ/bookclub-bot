package com.bookclub.poll.infrastructure.persistence

import java.io.Serializable

data class PollParticipantId(
    var pollRoundId: Long = 0,
    var telegramUserId: Long = 0
) : Serializable