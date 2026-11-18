package com.bookclub.poll.infrastructure.persistence

import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "poll_participants")
@IdClass(PollParticipantId::class)
class PollParticipantEntity(

    @Id
    @Column(name = "poll_round_id")
    var pollRoundId: Long = 0,

    @Id
    @Column(name = "telegram_user_id")
    var telegramUserId: Long = 0,

    @Column(name = "answered_at", nullable = false)
    var answeredAt: Instant = Instant.now()
)