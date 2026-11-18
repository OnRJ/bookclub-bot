
package com.bookclub.poll.application

import com.bookclub.poll.infrastructure.persistence.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

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

    fun saveAnswerAndCheckCompletion(
        telegramPollId: String,
        telegramUserId: Long,
        selectedOptionIds: List<Int>
    ) {
        val round = pollRounds.findByTelegramPollId(telegramPollId)
            ?: return

        if (round.status != "ACTIVE") {
            println(
                "Ignoring answer for inactive poll: $telegramPollId, " +
                        "status=${round.status}"
            )
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

        check(pollRound.status == "ACTIVE") {
            "Poll round is not active"
        }

        val isEligible =
            eligibleVoters.existsByPollRoundIdAndTelegramUserId(
                pollRoundId,
                telegramUserId
            )

        if (!isEligible) {
            println("User $telegramUserId is not eligible to vote")
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
