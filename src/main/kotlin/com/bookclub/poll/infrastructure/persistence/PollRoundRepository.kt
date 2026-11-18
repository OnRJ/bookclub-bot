package com.bookclub.poll.infrastructure.persistence

import jakarta.persistence.LockModeType
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Lock
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

interface PollRoundRepository : JpaRepository<PollRoundEntity, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM PollRoundEntity p WHERE p.id = :id")
    fun findByIdForUpdate(@Param("id") id: Long): PollRoundEntity?

    fun findByTelegramPollId(telegramPollId: String): PollRoundEntity?

    fun findAllByStatusAndClosesAtLessThanEqual(
        status: String,
        closesAt: Instant
    ): List<PollRoundEntity>

    fun findBySubmissionIdAndRoundNumber(
        submissionId: Long,
        roundNumber: Int
    ): PollRoundEntity?

    fun findAllByStatusAndClosingStartedAtLessThanEqual(
        status: String,
        closingStartedAt: Instant
    ): List<PollRoundEntity>

    @Modifying(
        flushAutomatically = true,
        clearAutomatically = true
    )
    @Transactional
    @Query("""
        UPDATE PollRoundEntity p
        SET p.status = 'CLOSING',
            p.closingStartedAt = CURRENT_TIMESTAMP
        WHERE p.id = :pollRoundId
          AND p.status = 'ACTIVE'
    """)
    fun claimForClosing(
        @Param("pollRoundId") pollRoundId: Long
    ): Int
}