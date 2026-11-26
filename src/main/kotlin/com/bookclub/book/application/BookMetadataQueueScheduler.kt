package com.bookclub.book.application

import com.bookclub.book.infrastructure.persistence.BookMetadataStatus
import com.bookclub.book.infrastructure.persistence.BookRepository
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.Duration
import org.slf4j.LoggerFactory

@Component
class BookMetadataQueueScheduler(
    private val books: BookRepository,
    private val asyncRunner: BookMetadataAsyncRunner
) {
    private val log = LoggerFactory.getLogger(javaClass)

    @Scheduled(fixedDelay = 2000)
    @Transactional
    fun dispatch() {
        val cutoff = Instant.now().minus(Duration.ofMinutes(2))

        val stuckBooks = books.findByMetadataStatusAndMetadataUpdatedAtBefore(
            BookMetadataStatus.PROCESSING,
            cutoff
        )

        val stuckWithoutTimestamp = books.findByMetadataStatusAndMetadataUpdatedAtIsNull(
            BookMetadataStatus.PROCESSING
        )

        val retryableFailedBooks = books.findByMetadataStatusAndMetadataUpdatedAtBefore(
            BookMetadataStatus.FAILED,
            Instant.now().minus(Duration.ofSeconds(1))
        )

        val retryablePartialBooks = books
            .findByMetadataStatusAndDescriptionIsNullAndMetadataUpdatedAtBefore(
                BookMetadataStatus.PARTIAL,
                Instant.now().minus(Duration.ofSeconds(1))
            )

        val allStuckBooks = (stuckBooks + stuckWithoutTimestamp + retryableFailedBooks + retryablePartialBooks)
            .distinctBy { it.id }

        allStuckBooks.forEach {
            log.warn(
                "Resetting book {} from {} to NEW after metadata timeout/retry window",
                it.id,
                it.metadataStatus
            )
            it.metadataStatus = BookMetadataStatus.NEW
            it.metadataUpdatedAt = null
        }

        if (allStuckBooks.isNotEmpty()) {
            books.saveAll(allStuckBooks)
        }

        val batch = books.findByMetadataStatus(
            BookMetadataStatus.NEW,
            PageRequest.of(0, 10)
        )

        if (batch.isEmpty()) return

        val processingTime = Instant.now()

        batch.forEach {
            it.metadataStatus = BookMetadataStatus.PROCESSING
            it.metadataUpdatedAt = processingTime
        }

        books.saveAll(batch)

        batch.forEach {
            asyncRunner.run(requireNotNull(it.id))
        }
    }
}
