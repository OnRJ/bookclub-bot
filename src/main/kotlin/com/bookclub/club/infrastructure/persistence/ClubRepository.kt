package com.bookclub.club.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface ClubRepository : JpaRepository<ClubEntity, Long> {
    fun findByTelegramChatId(telegramChatId: Long): ClubEntity?
}
