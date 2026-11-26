package com.bookclub.book.infrastructure.persistence

import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface BookRepository : JpaRepository<BookEntity, Long> {
    fun findFirstByTitleIgnoreCase(title: String): BookEntity?

    fun findByMetadataStatus(
        status: BookMetadataStatus
    ): List<BookEntity>

    fun findByMetadataStatus(
        status: BookMetadataStatus,
        pageable: Pageable
    ): List<BookEntity>

    fun findByMetadataStatusAndMetadataUpdatedAtBefore(
        status: BookMetadataStatus,
        cutoff: Instant
    ): List<BookEntity>


    fun findByMetadataStatusAndMetadataUpdatedAtIsNull(status: BookMetadataStatus): List<BookEntity>
}
