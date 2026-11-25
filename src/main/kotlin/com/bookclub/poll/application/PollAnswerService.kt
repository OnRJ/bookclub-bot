
package com.bookclub.poll.application

import com.bookclub.poll.domain.PollRoundStatus

import com.bookclub.poll.infrastructure.persistence.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.slf4j.LoggerFactory

@Service
class PollAnswerService(
    private val pollRounds: PollRoundRepository,
    private val pollRoundOptions: PollRoundOptionRepository,
    private val eligibleVoters: PollEligibleVoterRepository,
    private val participants: PollParticipantRepository,
    private val votes: PollVoteRepository,
    private val pollCompletionService: PollCompletionService,
    private val pollClosingService: PollClosingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun saveAnswerAndCheckCompletion(
        telegramPollId: String,
        telegramUserId: Long,
        selectedOptionIds: List<Int>
    ) {
        val round = pollRounds.findByTelegramPollId(telegramPollId)
            ?: return

        if (PollRoundStatus.fromPersisted(round.status) != PollRoundStatus.ACTIVE) {
            log.info("Ignoring answer for inactive poll {} with status {}", telegramPollId, round.status)
            return
        }

        saveAnswer(
            telegramPollId,
            telegramUserId,
            selectedOptionIds
        )

        val roundId = requireNotNull(round.id)

        if (pollCompletionService.areAllVotersFinished(roundId)) {
            pollClosingService.closePoll(roundId)
        }
    }

    @Transactional
    fun saveAnswer(
        telegramPollId: String,
        telegramUserId: Long,
        selectedOptionIds: List<Int>
    ) {
        val pollRound = pollRounds.findByTelegramPollId(telegramPollId)
            ?: throw IllegalArgumentException(
                "Poll round not found: $telegramPollId"
            )

        val pollRoundId = requireNotNull(pollRound.id)

        check(PollRoundStatus.fromPersisted(pollRound.status) == PollRoundStatus.ACTIVE) {
            "Poll round is not active"
        }

        val isEligible =
            eligibleVoters.existsByPollRoundIdAndTelegramUserId(
                pollRoundId,
                telegramUserId
            )

        if (!isEligible) {
            log.info("User {} is not eligible to vote in poll {}", telegramUserId, telegramPollId)
            return
        }

        val selectedOptions = selectedOptionIds.map { index ->
            pollRoundOptions.findByPollRoundIdAndOptionIndex(
                pollRoundId,
                index
            ) ?: throw IllegalArgumentException(
                "Option $index not found"
            )
        }

        val alreadyAnswered =
            participants.existsByPollRoundIdAndTelegramUserId(
                pollRoundId,
                telegramUserId
            )

        if (!alreadyAnswered) {
            participants.save(
                PollParticipantEntity(
                    pollRoundId = pollRoundId,
                    telegramUserId = telegramUserId
                )
            )
        }

        votes.deleteAllByPollRoundIdAndTelegramUserId(
            pollRoundId,
            telegramUserId
        )

        votes.saveAll(
            selectedOptions.map { option ->
                PollVoteEntity(
                    pollRoundId = pollRoundId,
                    telegramUserId = telegramUserId,
                    pollOptionId = requireNotNull(option.id)
                )
            }
        )
    }
}
