package com.bookclub.club.application

import com.bookclub.club.infrastructure.persistence.*
import com.bookclub.user.infrastructure.persistence.*
import io.mockk.*
import org.junit.jupiter.api.Test

class ClubMemberRegistrationServiceTest {
    private val clubs = mockk<ClubRepository>()
    private val members = mockk<ClubMemberRepository>()
    private val users = mockk<UserRepository>()
    private val service = ClubMemberRegistrationService(clubs, members, users)

    @Test
    fun `does nothing for unknown club`() {
        every { clubs.findByTelegramChatId(-1L) } returns null
        service.registerMember(-1L, 2L, null, "Reader")
        verify(exactly = 0) { users.findByTelegramUserId(any()) }
    }

    @Test
    fun `creates user and membership`() {
        val club = ClubEntity(telegramChatId = -1L, name = "Readers")
        val user = UserEntity(telegramUserId = 2L, firstName = "Reader")
        every { clubs.findByTelegramChatId(-1L) } returns club
        every { users.findByTelegramUserId(2L) } returns null
        every { users.save(any()) } returns user
        every { members.existsByClubTelegramChatIdAndUserTelegramUserId(-1L, 2L) } returns false
        every { members.save(any()) } answers { firstArg() }

        service.registerMember(-1L, 2L, "reader", "Reader")

        verify { users.save(any()) }
        verify { members.save(match { it.club == club && it.user == user }) }
    }
}
