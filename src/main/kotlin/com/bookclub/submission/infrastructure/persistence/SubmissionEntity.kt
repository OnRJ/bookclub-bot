package com.bookclub.submission.infrastructure.persistence

import com.bookclub.book.infrastructure.persistence.BookEntity
import com.bookclub.club.infrastructure.persistence.ClubEntity
import com.bookclub.user.infrastructure.persistence.UserEntity
import jakarta.persistence.*
import java.time.Instant
import java.time.LocalDate

@Entity
@Table(name = "submissions")
class SubmissionEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "club_id") var club: ClubEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "submitted_by") var submittedBy: UserEntity,
    @Column(name = "reading_month", nullable = false) var readingMonth: LocalDate,
    @Column(nullable = false) var status: String = "DRAFT",
    @Column(name = "created_at", nullable = false) var createdAt: Instant = Instant.now(),
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "winner_book_id")
    var winnerBook: BookEntity? = null,

    @Column(name = "winner_selected_at")
    var winnerSelectedAt: Instant? = null
)
