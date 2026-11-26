package com.bookclub.infrastructure.telegram

import com.bookclub.book.infrastructure.persistence.BookMetadataStatus
import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import com.bookclub.poll.infrastructure.persistence.PollRoundOptionRepository
import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Component
class BookCatalogPublicationScheduler(
    private val pollRounds: PollRoundRepository,
    private val pollOptions: PollRoundOptionRepository,
    private val publisher: TelegramPollPublisher
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 5_000)
    @Transactional
    fun publishReadyCatalogs() {
        pollRounds.findAllByCatalogPublishedAtIsNull().forEach { round ->
            val roundId = round.id ?: return@forEach
            val options = pollOptions.findAllByPollRoundIdOrderByOptionIndex(roundId)
            if (options.size != 10) return@forEach

            val processing = options.any { option ->
                option.book.metadataStatus == BookMetadataStatus.NEW ||
                    option.book.metadataStatus == BookMetadataStatus.PROCESSING
            }
            if (processing) return@forEach

            try {
                val published = publisher.publishCatalogButtonSafely(
                    com.bookclub.poll.application.PollRoundResult(
                        pollRoundId = roundId,
                        submissionId = requireNotNull(round.submission.id),
                        roundNumber = round.roundNumber,
                        telegramChatId = round.telegramChatId,
                        bookTitles = options.map { it.book.title },
                        eligibleVotersCount = 0
                    )
                )
                if (published) {
                    round.catalogPublishedAt = Instant.now()
                    pollRounds.save(round)
                }
            } catch (exception: Exception) {
                log.error("Failed to publish catalog for poll round {}", roundId, exception)
            }
        }
    }
}
