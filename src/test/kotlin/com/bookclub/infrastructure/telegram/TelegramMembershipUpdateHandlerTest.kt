package com.bookclub.infrastructure.telegram

import com.bookclub.club.application.ClubMemberRegistrationService
import com.bookclub.club.application.ClubRegistrationService
import com.pengrad.telegrambot.TelegramBot
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test

class TelegramMembershipUpdateHandlerTest {
    private val clubs = mockk<ClubRegistrationService>(relaxed = true)
    private val members = mockk<ClubMemberRegistrationService>(relaxed = true)
    private val telegramBot = mockk<TelegramBot>(relaxed = true)

    private val handler = TelegramMembershipUpdateHandler(
        clubs = clubs,
        members = members,
        telegramBot = telegramBot,
        botUsername = "book_worm_club_bot"
    )

    @Test
    fun `registers group when a message arrives after missed membership update`() {
        handler.registerSender(-100L, 7L, "reader", "Reader")

        verify {
            clubs.registerOrActivate(-100L, "Book Club")
        }

        verify {
            members.registerMember(-100L, 7L, "reader", "Reader")
        }
    }

    @Test
    fun `ignores private chat sender for club registration`() {
        handler.registerSender(100L, 7L, "reader", "Reader")

        verify(exactly = 0) {
            clubs.registerOrActivate(any(), any())
        }

        verify(exactly = 0) {
            members.registerMember(any(), any(), any(), any())
        }
    }
}