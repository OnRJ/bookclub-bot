package com.bookclub.book.application

import com.bookclub.book.infrastructure.persistence.BookMetadataStatus
import com.bookclub.book.infrastructure.persistence.BookRepository
import org.springframework.data.domain.PageRequest
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.Instant
import java.time.Duration

@Component
class BookMetadataQueueScheduler(
    private val books: BookRepository,
    private val asyncRunner: BookMetadataAsyncRunner
) {

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
            Instant.now().minus(Duration.ofSeconds(5))
        )

        val allStuckBooks = (stuckBooks + stuckWithoutTimestamp + retryableFailedBooks)
            .distinctBy { it.id }

        allStuckBooks.forEach {
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
