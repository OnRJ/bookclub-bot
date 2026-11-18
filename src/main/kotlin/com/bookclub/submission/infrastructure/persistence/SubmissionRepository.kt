package com.bookclub.submission.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import java.time.LocalDate

interface SubmissionRepository : JpaRepository<SubmissionEntity, Long> {
    fun existsByClubTelegramChatIdAndReadingMonth(chatId: Long, month: LocalDate): Boolean

    fun findByIdAndClubTelegramChatId(
        id: Long,
        telegramChatId: Long
    ): SubmissionEntity?
}
