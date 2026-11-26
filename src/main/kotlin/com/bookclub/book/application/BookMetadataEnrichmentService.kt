package com.bookclub.book.application

import com.bookclub.book.infrastructure.persistence.BookMetadataStatus
import com.bookclub.book.infrastructure.persistence.BookRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.Instant

@Service
class BookMetadataEnrichmentService(
    private val books: BookRepository,
    private val metadataResolver: BookMetadataResolver,
    private val descriptionGenerator: BookDescriptionGenerator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun enrich(bookId: Long) {
        val book = books.findById(bookId).orElse(null) ?: return

        if (book.metadataStatus != BookMetadataStatus.NEW &&
            book.metadataStatus != BookMetadataStatus.PROCESSING
        ) {
            return
        }

        try {
            book.metadataStatus = BookMetadataStatus.PROCESSING
            books.save(book)

            val resolution = metadataResolver.resolve(book.title, book.author)

            if (resolution == null) {
                val generated = descriptionGenerator.generate(
                    BookDescriptionRequest(
                        title = book.title,
                        author = book.author,
                        sourceDescription = null,
                        pageCount = book.pageCount,
                        isbn = book.isbn,
                        metadataSource = "AI_FALLBACK"
                    )
                )
                applyAiMetadata(book, generated)
                if (generated.success && !generated.description.isNullOrBlank()) {
                    book.description = generated.description
                    book.descriptionSource = "AI_FALLBACK"
                    book.metadataStatus = BookMetadataStatus.PARTIAL
                } else {
                    book.metadataStatus = BookMetadataStatus.FAILED
                }
                book.metadataUpdatedAt = Instant.now()
                books.save(book)
                return
            }

            val candidate = resolution.candidate

            if (book.author.isNullOrBlank() && !candidate.author.isNullOrBlank()) {
                book.author = candidate.author
            }

            if (book.pageCount == null && candidate.pageCount != null) {
                book.pageCount = candidate.pageCount
            }

            if (book.coverUrl.isNullOrBlank() && !candidate.coverUrl.isNullOrBlank()) {
                book.coverUrl = candidate.coverUrl
            }

            if (book.isbn.isNullOrBlank() && !candidate.isbn.isNullOrBlank()) {
                book.isbn = candidate.isbn
            }

            if (!book.description.isNullOrBlank()) {
                book.metadataStatus = BookMetadataStatus.READY
            } else if (!candidate.description.isNullOrBlank()) {
                val generated = descriptionGenerator.generate(
                    BookDescriptionRequest(
                        title = candidate.title,
                        author = book.author,
                        sourceDescription = candidate.description,
                        pageCount = book.pageCount,
                        isbn = book.isbn,
                        metadataSource = candidate.source
                    )
                )
                applyAiMetadata(book, generated)

                if (generated.success && !generated.description.isNullOrBlank()) {
                    book.description = generated.description
                    book.descriptionSource = candidate.source
                    book.metadataStatus = BookMetadataStatus.READY
                } else {
                    book.descriptionSource = candidate.source
                    book.metadataStatus = BookMetadataStatus.PARTIAL
                }
            } else {
                book.descriptionSource = candidate.source
                book.metadataStatus = BookMetadataStatus.PARTIAL
            }

            book.metadataUpdatedAt = Instant.now()
            books.save(book)

        } catch (exception: Exception) {
            log.error("BOOK_METADATA FAILED for book {}: {}", book.title, exception.message, exception)

            book.metadataStatus = BookMetadataStatus.FAILED
            book.metadataUpdatedAt = Instant.now()
            books.save(book)
        }
    }

    private fun applyAiMetadata(
        book: com.bookclub.book.infrastructure.persistence.BookEntity,
        generated: BookDescriptionGenerationResult
    ) {
        if (book.author.isNullOrBlank() && !generated.author.isNullOrBlank()) {
            book.author = generated.author
        }
        if (book.pageCount == null && generated.pageCount != null) {
            book.pageCount = generated.pageCount
        }
        if (book.coverUrl.isNullOrBlank() && !generated.coverUrl.isNullOrBlank()) {
            book.coverUrl = generated.coverUrl
        }
    }
}
