
package com.bookclub.poll.application

import com.bookclub.poll.domain.PollRoundStatus

import com.bookclub.poll.infrastructure.persistence.PollRoundOptionRepository
import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import com.bookclub.poll.infrastructure.persistence.PollVoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock

@Service
class PollResultService(
    private val pollRounds: PollRoundRepository,
    private val pollRoundOptions: PollRoundOptionRepository,
    private val votes: PollVoteRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    @Transactional
    fun calculateResult(pollRoundId: Long): PollResult {
        val pollRound = pollRounds.findByIdForUpdate(pollRoundId)
            ?: throw IllegalArgumentException("Poll round not found")

        check(PollRoundStatus.fromPersisted(pollRound.status) == PollRoundStatus.CLOSING) {
            "Poll round is not closing"
        }

        val roundId = requireNotNull(pollRound.id)

        val results = pollRoundOptions
            .findAllByPollRoundIdOrderByOptionIndex(roundId)
            .map { option ->
                PollOptionResult(
                    bookId = requireNotNull(option.book.id),
                    bookTitle = option.book.title,
                    votes = votes.countByPollRoundIdAndPollOptionId(
                        roundId,
                        requireNotNull(option.id)
                    ),
                    optionIndex = option.optionIndex
                )
            }

        val maxVotes = results.maxOfOrNull { it.votes } ?: 0L

        pollRound.closedAt = clock.instant()

        if (maxVotes == 0L) {
            pollRound.status = PollRoundStatus.ADMIN_DECISION.name

            return PollResult(
                pollRoundId = roundId,
                roundNumber = pollRound.roundNumber,
                status = PollRoundStatus.ADMIN_DECISION,
                winners = emptyList()
            )
        }

        val winners = results.filter { it.votes == maxVotes }

        val status = if (winners.size == 1) {
            val winner = winners.first()

            pollRound.status = PollRoundStatus.WON.name

            val submission = pollRound.submission

            submission.winnerBook = pollRoundOptions
                .findByPollRoundIdAndOptionIndex(
                    roundId,
                    winner.optionIndex
                )?.book ?: throw IllegalStateException(
                "Winning book not found"
            )

            submission.winnerSelectedAt = clock.instant()
            submission.status = "COMPLETED"

            PollRoundStatus.WON
        } else {
            if (pollRound.roundNumber == 1) {
                pollRound.status = PollRoundStatus.TIE.name
            } else {
                pollRound.status = PollRoundStatus.ADMIN_DECISION.name
            }

            PollRoundStatus.fromPersisted(pollRound.status)
        }

        return PollResult(
            pollRoundId = roundId,
            roundNumber = pollRound.roundNumber,
            status = status,
            winners = winners
        )
    }
}
