
package com.bookclub.poll.application

import com.bookclub.poll.infrastructure.persistence.PollRoundOptionRepository
import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import com.bookclub.poll.infrastructure.persistence.PollVoteRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class PollResultService(
    private val pollRounds: PollRoundRepository,
    private val pollRoundOptions: PollRoundOptionRepository,
    private val votes: PollVoteRepository
) {

    @Transactional
    fun calculateResult(pollRoundId: Long): PollResult {
        val pollRound = pollRounds.findByIdForUpdate(pollRoundId)
            ?: throw IllegalArgumentException("Poll round not found")

        check(pollRound.status == "CLOSING") {
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

        pollRound.closedAt = Instant.now()

        if (maxVotes == 0L) {
            pollRound.status = "ADMIN_DECISION"

            return PollResult(
                pollRoundId = roundId,
                roundNumber = pollRound.roundNumber,
                status = "ADMIN_DECISION",
                winners = emptyList()
            )
        }

        val winners = results.filter { it.votes == maxVotes }

        val status = if (winners.size == 1) {
            val winner = winners.first()

            pollRound.status = "WON"

            val submission = pollRound.submission

            submission.winnerBook = pollRoundOptions
                .findByPollRoundIdAndOptionIndex(
                    roundId,
                    winner.optionIndex
                )?.book ?: throw IllegalStateException(
                "Winning book not found"
            )

            submission.winnerSelectedAt = Instant.now()
            submission.status = "COMPLETED"

            "WON"
        } else {
            if (pollRound.roundNumber == 1) {
                pollRound.status = "TIE"
            } else {
                pollRound.status = "ADMIN_DECISION"
            }

            pollRound.status
        }

        return PollResult(
            pollRoundId = roundId,
            roundNumber = pollRound.roundNumber,
            status = status,
            winners = winners
        )
    }
}
