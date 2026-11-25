package com.bookclub.club.application

import com.bookclub.club.infrastructure.persistence.*
import io.mockk.*
import org.junit.jupiter.api.Test

class ClubRegistrationServiceTest {
    private val repository = mockk<ClubRepository>()
    private val service = ClubRegistrationService(repository)

    @Test
    fun `creates club when chat is new`() {
        every { repository.findByTelegramChatId(-1L) } returns null
        every { repository.save(any()) } answers { firstArg() }
        service.registerOrActivate(-1L, "Readers")
        verify { repository.save(match { it.telegramChatId == -1L && it.name == "Readers" }) }
    }

    @Test
    fun `updates existing club name`() {
        val club = ClubEntity(telegramChatId = -1L, name = "Old")
        every { repository.findByTelegramChatId(-1L) } returns club
        every { repository.save(club) } returns club
        service.registerOrActivate(-1L, "New")
        assert(club.name == "New")
        verify { repository.save(club) }
    }
}
