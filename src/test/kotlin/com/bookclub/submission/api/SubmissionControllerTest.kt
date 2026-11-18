package com.bookclub.submission.api

import com.bookclub.infrastructure.telegram.TelegramClubMembershipValidator
import com.bookclub.infrastructure.telegram.TelegramInitDataValidator
import com.bookclub.infrastructure.telegram.TelegramPollPublisher
import com.bookclub.infrastructure.telegram.ValidatedTelegramData
import com.bookclub.poll.application.PollRoundResult
import com.bookclub.poll.application.PollRoundService
import com.bookclub.submission.application.SubmitBooksCommand
import com.bookclub.submission.application.SubmitBooksUseCase
import com.bookclub.submission.application.SubmissionResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import java.time.YearMonth

class SubmissionControllerTest {

    private val validator = mockk<TelegramInitDataValidator>()
    private val useCase = mockk<SubmitBooksUseCase>()
    private val membershipValidator = mockk<TelegramClubMembershipValidator>()
    private val pollRoundService = mockk<PollRoundService>()
    private val telegramPollPublisher = mockk<TelegramPollPublisher>()

    private val controller = SubmissionController(
        validator,
        useCase,
        membershipValidator,
        pollRoundService,
        telegramPollPublisher
    )

    private val books = (1..10).map { "Book $it" }

    @Test
    fun `should submit books successfully`() {
        every {
            validator.validate("valid-init-data")
        } returns ValidatedTelegramData(
            telegramUserId = 123456L,
            firstName = "Alex",
            username = "alex",
            startParam = "club_-1001234567890"
        )

        every {
            membershipValidator.isMember(-1001234567890L, 123456L)
        } returns true

        every {
            useCase.execute(any())
        } returns SubmissionResult(
            submissionId = 15L,
            readingMonth = YearMonth.of(2026, 12),
            submittedByTelegramUserId = 123456L,
            bookTitles = books
        )

        val pollRound = mockk<PollRoundResult>(relaxed = true)

        every {
            pollRoundService.startFirstRound(any())
        } returns pollRound

        every {
            telegramPollPublisher.publishFirstRound(
                any(),
                any()
            )
        } returns Unit

        val response = controller.submit(
            SubmitBooksRequest(
                initData = "valid-init-data",
                bookTitles = books
            )
        )

        assertEquals(201, response.statusCode.value())
        assertEquals(15L, response.body?.submissionId)
        assertEquals(10, response.body?.bookCount)
        assertEquals("Список успешно отправлен!", response.body?.message)

        val command = slot<SubmitBooksCommand>()

        verify(exactly = 1) {
            useCase.execute(capture(command))
        }

        verify(exactly = 1) {
            pollRoundService.startFirstRound(
                match {
                    it.submissionId == 15L &&
                            it.telegramChatId == -1001234567890L
                }
            )
        }

        verify(exactly = 1) {
            telegramPollPublisher.publishFirstRound(
                pollRound,
                YearMonth.of(2026, 12)
            )
        }

        assertEquals(-1001234567890L, command.captured.telegramChatId)
        assertEquals(123456L, command.captured.telegramUserId)
        assertEquals("Alex", command.captured.firstName)
        assertEquals("alex", command.captured.username)
        assertEquals(books, command.captured.bookTitles)
    }

    @Test
    fun `should reject invalid club parameter`() {
        every {
            validator.validate("valid-init-data")
        } returns ValidatedTelegramData(
            telegramUserId = 123456L,
            firstName = "Alex",
            username = null,
            startParam = "invalid"
        )

        assertThrows(IllegalArgumentException::class.java) {
            controller.submit(
                SubmitBooksRequest(
                    initData = "valid-init-data",
                    bookTitles = books
                )
            )
        }

        verify(exactly = 0) {
            useCase.execute(any())
        }
    }

    @Test
    fun `should reject user who is not a club member`() {
        every {
            validator.validate("valid-init-data")
        } returns ValidatedTelegramData(
            telegramUserId = 123456L,
            firstName = "Alex",
            username = "alex",
            startParam = "club_-1001234567890"
        )

        every {
            membershipValidator.isMember(-1001234567890L, 123456L)
        } returns false

        assertThrows(IllegalArgumentException::class.java) {
            controller.submit(
                SubmitBooksRequest(
                    initData = "valid-init-data",
                    bookTitles = books
                )
            )
        }

        verify(exactly = 0) {
            useCase.execute(any())
        }
    }
}
