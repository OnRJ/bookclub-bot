package com.bookclub.poll.infrastructure.persistence

import jakarta.persistence.*

@Entity
@Table(name = "poll_eligible_voters")
@IdClass(PollEligibleVoterId::class)
class PollEligibleVoterEntity(

    @Id
    @Column(name = "poll_round_id")
    var pollRoundId: Long = 0,

    @Id
    @Column(name = "telegram_user_id")
    var telegramUserId: Long = 0
)