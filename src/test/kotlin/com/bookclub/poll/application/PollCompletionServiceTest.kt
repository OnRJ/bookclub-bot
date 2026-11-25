package com.bookclub.poll.application

import com.bookclub.poll.infrastructure.persistence.*
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class PollCompletionServiceTest {
    private val eligible = mockk<PollEligibleVoterRepository>()
    private val participants = mockk<PollParticipantRepository>()
    private val service = PollCompletionService(mockk(), eligible, participants)

    @Test
    fun `returns true when all eligible voters answered`() {
        every { eligible.countByPollRoundId(1L) } returns 3L
        every { participants.countByPollRoundId(1L) } returns 3L
        assertTrue(service.areAllVotersFinished(1L))
    }

    @Test
    fun `returns false when nobody or only some voters answered`() {
        every { eligible.countByPollRoundId(1L) } returns 3L
        every { participants.countByPollRoundId(1L) } returns 2L
        assertFalse(service.areAllVotersFinished(1L))

        every { eligible.countByPollRoundId(2L) } returns 0L
        every { participants.countByPollRoundId(2L) } returns 0L
        assertFalse(service.areAllVotersFinished(2L))
    }
}
