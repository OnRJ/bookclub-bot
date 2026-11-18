package com.bookclub.poll.application

import com.bookclub.book.infrastructure.persistence.BookEntity
import com.bookclub.club.infrastructure.persistence.ClubMemberEntity
import com.bookclub.club.infrastructure.persistence.ClubMemberRepository
import com.bookclub.poll.application.PollRoundService
import com.bookclub.poll.application.StartPollCommand
import com.bookclub.poll.infrastructure.persistence.*
import com.bookclub.submission.infrastructure.persistence.*
import com.bookclub.user.infrastructure.persistence.UserEntity
import io.mockk.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PollRoundServiceTest {

    private val submissions = mockk<SubmissionRepository>()
    private val submissionBooks = mockk<SubmissionBookRepository>()
    private val clubMembers = mockk<ClubMemberRepository>()
    private val pollRounds = mockk<PollRoundRepository>()
    private val pollRoundOptions = mockk<PollRoundOptionRepository>()
    private val eligibleVoters = mockk<PollEligibleVoterRepository>()

    private val service = PollRoundService(
        submissions,
        submissionBooks,
        clubMembers,
        pollRounds,
        pollRoundOptions,
        eligibleVoters
    )

    @Test
    fun `should create first poll round with books and eligible voters`() {
        val submissionId = 1L
        val chatId = -5520754325L

        val submission = mockk<SubmissionEntity>()

        every {
            submissions.findByIdAndClubTelegramChatId(submissionId, chatId)
        } returns submission

        every {
            pollRounds.findBySubmissionIdAndRoundNumber(submissionId, 1)
        } returns null

        val books = (1..10).map { index ->
            val book = mockk<BookEntity>()
            every { book.title } returns "Book $index"

            val submissionBook = mockk<SubmissionBookEntity>()
            every { submissionBook.book } returns book

            submissionBook
        }

        every {
            submissionBooks.findAllBySubmissionIdOrderByPosition(submissionId)
        } returns books

        val members = (1..3).map { index ->
            val user = mockk<UserEntity>()
            every { user.telegramUserId } returns index.toLong()

            val member = mockk<ClubMemberEntity>()
            every { member.user } returns user

            member
        }

        every {
            clubMembers.findAllByClubTelegramChatId(chatId)
        } returns members

        every {
            pollRounds.save(any())
        } answers {
            firstArg<PollRoundEntity>().apply { id = 100L }
        }

        every {
            pollRoundOptions.saveAll(any<Iterable<PollRoundOptionEntity>>())
        } answers { firstArg() }

        every {
            eligibleVoters.saveAll(any<Iterable<PollEligibleVoterEntity>>())
        } answers { firstArg() }

        val result = service.startFirstRound(
            StartPollCommand(
                submissionId = submissionId,
                telegramChatId = chatId
            )
        )

        assertEquals(100L, result.pollRoundId)
        assertEquals(submissionId, result.submissionId)
        assertEquals(1, result.roundNumber)
        assertEquals(chatId, result.telegramChatId)
        assertEquals(10, result.bookTitles.size)
        assertEquals(3, result.eligibleVotersCount)

        assertEquals(
            (1..10).map { "Book $it" },
            result.bookTitles
        )

        verify(exactly = 1) {
            pollRoundOptions.saveAll(any<Iterable<PollRoundOptionEntity>>())
        }

        verify(exactly = 1) {
            eligibleVoters.saveAll(any<Iterable<PollEligibleVoterEntity>>())
        }
    }
}
