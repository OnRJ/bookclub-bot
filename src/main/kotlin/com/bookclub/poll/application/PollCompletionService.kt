package com.bookclub.poll.application

import com.bookclub.poll.infrastructure.persistence.PollEligibleVoterRepository
import com.bookclub.poll.infrastructure.persistence.PollParticipantRepository
import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class PollCompletionService(
    private val pollRounds: PollRoundRepository,
    private val eligibleVoters: PollEligibleVoterRepository,
    private val participants: PollParticipantRepository
) {

    @Transactional(readOnly = true)
    fun areAllVotersFinished(pollRoundId: Long): Boolean {
        val eligibleCount = eligibleVoters.countByPollRoundId(pollRoundId)
        val answeredCount = participants.countByPollRoundId(pollRoundId)

        return eligibleCount in 1..answeredCount
    }
}