package com.bookclub.poll.application

import com.bookclub.poll.domain.PollRoundStatus

import com.bookclub.poll.infrastructure.persistence.PollRoundEntity
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.Optional

class PollClosingServiceTest {
    private val rounds = mockk<com.bookclub.poll.infrastructure.persistence.PollRoundRepository>()
    private val results = mockk<PollResultService>()
    private val roundService = mockk<PollRoundService>(relaxed = true)
    private val gateway = mockk<TelegramGateway>(relaxed = true)
    private val notifications = mockk<PollAdminNotificationService>(relaxed = true)
    private val service = PollClosingService(rounds, results, roundService, gateway, notifications)

    @Test
    fun `does nothing when another worker already claimed closing`() {
        every { rounds.claimForClosing(1L) } returns 0
        service.closePoll(1L)
        verify(exactly = 0) { gateway.stopPoll(any(), any()) }
    }

    @Test
    fun `announces winner after successful stop`() {
        val round = round()
        every { rounds.claimForClosing(1L) } returns 1
        every { rounds.findById(1L) } returns Optional.of(round)
        every { gateway.stopPoll(-1L, 55L) } returns TelegramPollPublication(true, null)
        every { results.calculateResult(1L) } returns PollResult(1L, 1, PollRoundStatus.WON, listOf(PollOptionResult(2L, "Dune", 0, 4)))

        service.closePoll(1L)

        verify { gateway.sendMessage(-1L, match { it.contains("Dune") }) }
    }

    @Test
    fun `moves failed runoff publication to admin decision`() {
        val round = round()
        val runoff = PollRoundResult(2L, 3L, 2, -1L, listOf("Dune", "1984"), 2)
        every { rounds.claimForClosing(1L) } returns 1
        every { rounds.findById(1L) } returns Optional.of(round)
        every { gateway.stopPoll(-1L, 55L) } returns TelegramPollPublication(true, null)
        every { results.calculateResult(1L) } returns PollResult(1L, 1, PollRoundStatus.TIE, listOf(
            PollOptionResult(2L, "Dune", 0, 2), PollOptionResult(3L, "1984", 1, 2)
        ))
        every { roundService.startRunoffRound(1L, listOf(2L, 3L)) } returns runoff
        every { gateway.sendPoll(-1L, any(), any()) } returns TelegramPollPublication(false, "Telegram unavailable")

        assertDoesNotThrow { service.closePoll(1L) }
        verify { roundService.markRoundForAdminDecision(2L) }
        verify { notifications.notifyAdmins(-1L, match { it.contains("Нужен выбор") }) }
    }

    private fun round() = PollRoundEntity(
        submission = mockk(), telegramChatId = -1L, roundNumber = 1,
        status = "ACTIVE", closesAt = Instant.now(), telegramMessageId = 55L
    ).apply { id = 1L }
}
