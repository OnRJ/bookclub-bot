package com.bookclub.poll.application

import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Duration

@Component
class PollDeadlineScheduler(
    private val pollRounds: PollRoundRepository,
    private val pollClosingService: PollClosingService
) {

    @Scheduled(fixedDelay = 60_000)
    fun closeExpiredPolls() {
        val expiredRounds =
            pollRounds.findAllByStatusAndClosesAtLessThanEqual(
                "ACTIVE",
                Instant.now()
            )

        expiredRounds.forEach { round ->
            val roundId = round.id ?: return@forEach

            try {
                pollClosingService.closePoll(roundId)
            } catch (exception: Exception) {
                println(
                    "Failed to close poll round $roundId: ${exception.message}"
                )
            }
        }
    }

    @Scheduled(fixedDelay = 300_000)
    fun reportStuckClosingRounds() {
        val threshold = Instant.now().minus(Duration.ofMinutes(10))

        val stuckRounds =
            pollRounds.findAllByStatusAndClosingStartedAtLessThanEqual(
                "CLOSING",
                threshold
            )

        stuckRounds.forEach { round ->
            println(
                "WARNING: Poll round ${round.id} has been in CLOSING " +
                        "since ${round.closingStartedAt}. Manual recovery required."
            )
        }
    }
}