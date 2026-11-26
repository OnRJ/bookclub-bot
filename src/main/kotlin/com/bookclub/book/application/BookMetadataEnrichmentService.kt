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
    private val descriptionGenerator: BookDescriptionGenerator,
    private val googleBooksProvider: GoogleBooksMetadataProvider
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun enrich(bookId: Long) {
        val book = books.findById(bookId).orElse(null) ?: return
        var stage = "LOAD_BOOK"

        if (book.metadataStatus != BookMetadataStatus.NEW &&
            book.metadataStatus != BookMetadataStatus.PROCESSING
        ) {
            return
        }

        try {
            stage = "GOOGLE_SEARCH"
            book.metadataStage = stage
            book.metadataStatus = BookMetadataStatus.PROCESSING
            books.save(book)

            val resolution = metadataResolver.resolve(book.title, book.author)

            if (resolution == null) {
                stage = "AI_GENERATION"
                book.metadataStage = stage
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
                stage = "GOOGLE_RETRY"
                book.metadataStage = stage
                retryGoogleMetadata(book)
                if (generated.success && !generated.description.isNullOrBlank()) {
                    book.description = generated.description
                    book.descriptionSource = "AI_FALLBACK"
                    book.metadataStatus = BookMetadataStatus.PARTIAL
                } else {
                    book.metadataStatus = BookMetadataStatus.FAILED
                    book.metadataError = generated.reason ?: "AI did not return a description"
                }
                stage = "SAVE_RESULT"
                book.metadataStage = stage
                book.metadataUpdatedAt = Instant.now()
                books.save(book)
                return
            }

            val candidate = resolution.candidate

            if (shouldReplaceAuthor(book.author, candidate.author)) {
                book.author = candidate.author
            }

            if (book.pageCount == null && candidate.pageCount != null) {
                book.pageCount = candidate.pageCount
            }

            if (book.isbn.isNullOrBlank() && !candidate.isbn.isNullOrBlank()) {
                book.isbn = candidate.isbn
            }

            if (!book.description.isNullOrBlank()) {
                book.metadataStatus = BookMetadataStatus.READY
            } else if (!candidate.description.isNullOrBlank()) {
                stage = "AI_GENERATION"
                book.metadataStage = stage
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
                    if (!candidate.description.isBlank()) {
                        book.description = candidate.description
                        book.descriptionSource = candidate.source
                        book.metadataStatus = BookMetadataStatus.READY
                    } else {
                        book.descriptionSource = candidate.source
                        book.metadataStatus = BookMetadataStatus.PARTIAL
                    }
                }
            } else {
                book.descriptionSource = candidate.source
                stage = "AI_GENERATION"
                book.metadataStage = stage
                val generated = descriptionGenerator.generate(
                    BookDescriptionRequest(
                        title = candidate.title,
                        author = book.author,
                        sourceDescription = candidate.description,
                        pageCount = book.pageCount,
                        isbn = book.isbn,
                        metadataSource = "AI_FALLBACK"
                    )
                )
                applyAiMetadata(book, generated)
                if (generated.success && !generated.description.isNullOrBlank()) {
                    book.description = generated.description
                    book.descriptionSource = "AI_FALLBACK"
                }
                book.metadataStatus = BookMetadataStatus.PARTIAL
            }

            stage = "SAVE_RESULT"
            book.metadataStage = stage
            book.metadataUpdatedAt = Instant.now()
            books.save(book)

        } catch (exception: Exception) {
            book.metadataStage = stage
            log.error("BOOK_METADATA FAILED at {} for book {}: {}", stage, book.title, exception.message, exception)

            book.metadataStatus = BookMetadataStatus.FAILED
            book.metadataError = "$stage: ${exception.message ?: exception.javaClass.simpleName}"
            book.metadataUpdatedAt = Instant.now()
            books.save(book)
        }
    }

    private fun applyAiMetadata(
        book: com.bookclub.book.infrastructure.persistence.BookEntity,
        generated: BookDescriptionGenerationResult
    ) {
        if (shouldReplaceAuthor(book.author, generated.author)) {
            book.author = generated.author
        }
        if (book.pageCount == null && generated.pageCount != null) {
            book.pageCount = generated.pageCount
        }
    }

    private fun retryGoogleMetadata(book: com.bookclub.book.infrastructure.persistence.BookEntity) {
        try {
            val candidate = googleBooksProvider
                .search(book.title, book.author)
                .firstOrNull { it.pageCount != null || it.isbn != null }
                ?: return

            if (shouldReplaceAuthor(book.author, candidate.author)) {
                book.author = candidate.author
            }
            if (book.pageCount == null) book.pageCount = candidate.pageCount
            if (book.isbn.isNullOrBlank()) book.isbn = candidate.isbn
        } catch (exception: Exception) {
            log.warn("Google Books fallback failed for '{}': {}", book.title, exception.message)
        }
    }

    private fun shouldReplaceAuthor(current: String?, candidate: String?): Boolean {
        if (candidate.isNullOrBlank()) return false
        if (current.isNullOrBlank()) return true
        return !containsCyrillic(current) && containsCyrillic(candidate)
    }

    private fun containsCyrillic(value: String): Boolean =
        value.any { it in '\u0400'..'\u04FF' }
}
