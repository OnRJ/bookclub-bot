package com.bookclub.poll.infrastructure.persistence

import com.bookclub.book.infrastructure.persistence.BookEntity
import jakarta.persistence.*

@Entity
@Table(name = "poll_round_options")
class PollRoundOptionEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    var id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "poll_round_id", nullable = false)
    var pollRound: PollRoundEntity,

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "book_id", nullable = false)
    var book: BookEntity,

    @Column(name = "option_index", nullable = false)
    var optionIndex: Int
)