package com.bookclub.submission.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface SubmissionBookRepository : JpaRepository<SubmissionBookEntity, Long> {
    fun findAllBySubmissionIdOrderByPosition(
        submissionId: Long
    ): List<SubmissionBookEntity>
}
