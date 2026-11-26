package com.bookclub.book.infrastructure.persistence

import org.springframework.data.jpa.repository.JpaRepository

interface BookRepository : JpaRepository<BookEntity, Long> {
    fun findFirstByTitleIgnoreCase(title: String): BookEntity?
}