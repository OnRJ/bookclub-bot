package com.bookclub.poll.application

enum class PollRoundStatus {
    ACTIVE,
    CLOSING,
    CLOSED,
    WON,
    TIE,
    ADMIN_DECISION;

    companion object {
        fun fromPersisted(value: String): PollRoundStatus =
            entries.firstOrNull { it.name == value }
                ?: throw IllegalArgumentException("Unknown poll round status: $value")
    }
}
