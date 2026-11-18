package com.bookclub.submission.infrastructure.persistence

import com.bookclub.book.infrastructure.persistence.BookEntity
import jakarta.persistence.*

@Entity
@Table(name = "submission_books")
class SubmissionBookEntity(
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) var id: Long? = null,
    @ManyToOne(
        fetch = FetchType.LAZY,
        optional = false
    ) @JoinColumn(name = "submission_id") var submission: SubmissionEntity,
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "book_id") var book: BookEntity,
    @Column(nullable = false) var position: Int
)
