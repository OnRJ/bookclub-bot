package com.bookclub.club.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ClubMemberRepository : JpaRepository<ClubMemberEntity, Long> {

    fun findAllByClubTelegramChatId(
        telegramChatId: Long
    ): List<ClubMemberEntity>

    fun existsByClubTelegramChatIdAndUserTelegramUserId(
        telegramChatId: Long,
        telegramUserId: Long
    ): Boolean

    @Query("""
        SELECT cm
        FROM ClubMemberEntity cm
        JOIN FETCH cm.user
        WHERE cm.club.telegramChatId = :telegramChatId
          AND cm.role = :role
    """)
    fun findAllByClubTelegramChatIdAndRoleWithUser(
        @Param("telegramChatId") telegramChatId: Long,
        @Param("role") role: String
    ): List<ClubMemberEntity>
}
