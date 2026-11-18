package com.bookclub.poll.infrastructure.persistence

import jakarta.persistence.*

@Entity
@Table(name = "poll_votes")
@IdClass(PollVoteId::class)
class PollVoteEntity(

    @Id
    @Column(name = "poll_round_id")
    var pollRoundId: Long = 0,

    @Id
    @Column(name = "telegram_user_id")
    var telegramUserId: Long = 0,

    @Id
    @Column(name = "poll_option_id")
    var pollOptionId: Long = 0
)