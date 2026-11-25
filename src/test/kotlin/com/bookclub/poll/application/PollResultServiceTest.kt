package com.bookclub.poll.application

import com.bookclub.poll.domain.PollRoundStatus

import com.bookclub.book.infrastructure.persistence.BookEntity
import com.bookclub.poll.infrastructure.persistence.*
import com.bookclub.submission.infrastructure.persistence.SubmissionEntity
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

class PollResultServiceTest {
    private val rounds = mockk<PollRoundRepository>()
    private val options = mockk<PollRoundOptionRepository>()
    private val votes = mockk<PollVoteRepository>()
    private val clock = Clock.fixed(Instant.parse("2026-11-25T12:00:00Z"), ZoneOffset.UTC)
    private val service = PollResultService(rounds, options, votes, clock)

    @Test
    fun `marks unique winner and submission completed`() {
        val submission = SubmissionEntity(
            id = 1L,
            club = mockk(),
            submittedBy = mockk(),
            readingMonth = LocalDate.of(2026, 12, 1)
        )
        val round = PollRoundEntity(submission = submission, telegramChatId = -1L, roundNumber = 1, status = "CLOSING", closesAt = clock.instant()).apply { id = 2L }
        val book = BookEntity(id = 3L, title = "Dune")
        val option = PollRoundOptionEntity(id = 4L, pollRound = round, book = book, optionIndex = 0)
        every { rounds.findByIdForUpdate(2L) } returns round
        every { options.findAllByPollRoundIdOrderByOptionIndex(2L) } returns listOf(option)
        every { votes.countByPollRoundIdAndPollOptionId(2L, 4L) } returns 5L
        every { options.findByPollRoundIdAndOptionIndex(2L, 0) } returns option

        val result = service.calculateResult(2L)

        assertEquals(PollRoundStatus.WON, result.status)
        assertEquals("WON", round.status)
        assertEquals("COMPLETED", submission.status)
        assertEquals(book, submission.winnerBook)
        assertEquals(clock.instant(), submission.winnerSelectedAt)
    }

    @Test
    fun `marks first round tie`() {
        val round = round(roundNumber = 1)
        val optionsList = (0..1).map { index -> option(round, index, index + 1L, "Book $index") }
        every { rounds.findByIdForUpdate(2L) } returns round
        every { options.findAllByPollRoundIdOrderByOptionIndex(2L) } returns optionsList
        optionsList.forEach { option -> every { votes.countByPollRoundIdAndPollOptionId(2L, option.id!!) } returns 2L }

        val result = service.calculateResult(2L)

        assertEquals(PollRoundStatus.TIE, result.status)
        assertEquals(2, result.winners.size)
    }

    @Test
    fun `sends empty poll to admin decision`() {
        val round = round(roundNumber = 2)
        val option = option(round, 0, 1L, "Book")
        every { rounds.findByIdForUpdate(2L) } returns round
        every { options.findAllByPollRoundIdOrderByOptionIndex(2L) } returns listOf(option)
        every { votes.countByPollRoundIdAndPollOptionId(2L, 1L) } returns 0L

        val result = service.calculateResult(2L)

        assertEquals(PollRoundStatus.ADMIN_DECISION, result.status)
        assertEquals(0, result.winners.size)
    }

    private fun round(roundNumber: Int) = PollRoundEntity(
        submission = mockk(), telegramChatId = -1L, roundNumber = roundNumber,
        status = "CLOSING", closesAt = clock.instant()
    ).apply { id = 2L }

    private fun option(round: PollRoundEntity, index: Int, id: Long, title: String) =
        PollRoundOptionEntity(id = id, pollRound = round, book = BookEntity(id = id, title = title), optionIndex = index)
}
