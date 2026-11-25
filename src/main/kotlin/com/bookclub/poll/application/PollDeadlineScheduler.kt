package com.bookclub.poll.application

import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.Instant
import java.time.Duration
import org.slf4j.LoggerFactory

@Component
class PollDeadlineScheduler(
    private val pollRounds: PollRoundRepository,
    private val pollClosingService: PollClosingService
) {
    private val log = LoggerFactory.getLogger(javaClass)

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
                log.error("Failed to close poll round {}: {}", roundId, exception.message, exception)
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
            log.warn("Poll round {} has been in CLOSING since {}. Manual recovery required.", round.id, round.closingStartedAt)
        }
    }
}
