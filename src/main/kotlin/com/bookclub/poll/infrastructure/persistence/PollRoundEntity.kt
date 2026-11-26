package com.bookclub.poll.infrastructure.persistence

import com.bookclub.submission.infrastructure.persistence.SubmissionEntity
import jakarta.persistence.*
import java.time.Instant

@Entity
@Table(name = "poll_rounds")
class PollRoundEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "submission_id", nullable = false)
    var submission: SubmissionEntity,

    @Column(name = "telegram_chat_id", nullable = false)
    var telegramChatId: Long,

    @Column(name = "telegram_poll_id")
    var telegramPollId: String? = null,

    @Column(name = "telegram_message_id")
    var telegramMessageId: Long? = null,

    @Column(name = "round_number", nullable = false)
    var roundNumber: Int,

    @Column(nullable = false)
    var status: String = "ACTIVE",

    @Column(name = "closes_at", nullable = false)
    var closesAt: Instant,

    @Column(name = "created_at", nullable = false, insertable = false, updatable = false)
    var createdAt: Instant? = null,

    @Column(name = "closed_at")
    var closedAt: Instant? = null,

    @Column(name = "closing_started_at")
    var closingStartedAt: Instant? = null,

    @Column(name = "catalog_published_at")
    var catalogPublishedAt: Instant? = null
)
