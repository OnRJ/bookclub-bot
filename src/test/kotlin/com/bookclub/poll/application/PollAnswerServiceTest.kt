package com.bookclub.poll.application

import com.bookclub.poll.infrastructure.persistence.*
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test

class PollAnswerServiceTest {
    private val rounds = mockk<PollRoundRepository>()
    private val options = mockk<PollRoundOptionRepository>()
    private val eligible = mockk<PollEligibleVoterRepository>()
    private val participants = mockk<PollParticipantRepository>()
    private val votes = mockk<PollVoteRepository>()
    private val completion = mockk<PollCompletionService>()
    private val closing = mockk<PollClosingService>(relaxed = true)
    private val service = PollAnswerService(rounds, options, eligible, participants, votes, completion, closing)

    @Test
    fun `ignores answers from users outside eligible snapshot`() {
        val round = mockk<PollRoundEntity> { every { id } returns 10L; every { status } returns "ACTIVE" }
        every { rounds.findByTelegramPollId("poll") } returns round
        every { eligible.existsByPollRoundIdAndTelegramUserId(10L, 7L) } returns false

        service.saveAnswer("poll", 7L, listOf(0))

        verify(exactly = 0) { participants.save(any()) }
        verify(exactly = 0) { options.findByPollRoundIdAndOptionIndex(any(), any()) }
    }

    @Test
    fun `rejects unknown option for eligible voter`() {
        val round = mockk<PollRoundEntity> { every { id } returns 10L; every { status } returns "ACTIVE" }
        every { rounds.findByTelegramPollId("poll") } returns round
        every { eligible.existsByPollRoundIdAndTelegramUserId(10L, 7L) } returns true
        every { options.findByPollRoundIdAndOptionIndex(10L, 99) } returns null

        assertThrows(IllegalArgumentException::class.java) {
            service.saveAnswer("poll", 7L, listOf(99))
        }
    }

    @Test
    fun `replaces previous votes and closes completed poll`() {
        val round = mockk<PollRoundEntity> { every { id } returns 10L; every { status } returns "ACTIVE" }
        val option = mockk<PollRoundOptionEntity> { every { id } returns 3L }
        every { rounds.findByTelegramPollId("poll") } returns round
        every { eligible.existsByPollRoundIdAndTelegramUserId(10L, 7L) } returns true
        every { options.findByPollRoundIdAndOptionIndex(10L, 0) } returns option
        every { participants.existsByPollRoundIdAndTelegramUserId(10L, 7L) } returns true
        every { completion.areAllVotersFinished(10L) } returns true
        every { participants.save(any()) } returns mockk()
        every { votes.deleteAllByPollRoundIdAndTelegramUserId(10L, 7L) } just Runs
        every { votes.saveAll(any<Iterable<PollVoteEntity>>()) } answers { firstArg() }

        service.saveAnswerAndCheckCompletion("poll", 7L, listOf(0))

        verify { votes.deleteAllByPollRoundIdAndTelegramUserId(10L, 7L) }
        verify { votes.saveAll(any<Iterable<PollVoteEntity>>()) }
        verify { closing.closePoll(10L) }
    }
}
