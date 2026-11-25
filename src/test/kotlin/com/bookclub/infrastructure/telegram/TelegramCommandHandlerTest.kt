package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.TelegramBot
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TelegramCommandHandlerTest {
    private val bot = mockk<TelegramBot>(relaxed = true)
    private val state = mockk<SubmissionConversationState>(relaxed = true)
    private val messages = mockk<SubmissionTelegramHandler>(relaxed = true)
    private val handler = TelegramCommandHandler(bot, "book_bot", state, messages)

    @Test
    fun `ignores ordinary messages`() {
        assertFalse(handler.handle(1L, 2L, null, "Reader", "hello"))
        verify(exactly = 0) { messages.sendMessage(any(), any()) }
    }

    @Test
    fun `handles help command`() {
        assertTrue(handler.handle(1L, 2L, null, "Reader", "/help"))
        verify { messages.sendMessage(1L, match { it.contains("/submit_books") }) }
    }

    @Test
    fun `starts submission from deep link`() {
        assertTrue(handler.handle(1L, 2L, null, "Reader", "/start submit_-100"))
        verify { state.start(2L, -100L) }
        verify { messages.sendMessage(1L, match { it.contains("10 книг") }) }
    }
}
