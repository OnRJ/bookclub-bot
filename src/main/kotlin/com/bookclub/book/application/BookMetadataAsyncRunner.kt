package com.bookclub.book.application

import org.springframework.scheduling.annotation.Async
import org.springframework.stereotype.Component

@Component
class BookMetadataAsyncRunner(
    private val enrichmentService: BookMetadataEnrichmentService
) {

    @Async("bookMetadataExecutor")
    fun run(bookId: Long) {
        enrichmentService.enrich(bookId)
    }
}
