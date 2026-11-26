package com.bookclub.book.infrastructure.persistence

enum class BookMetadataStatus {
    NEW,
    PROCESSING,
    READY,
    PARTIAL,
    FAILED
}