package com.bookclub.submission.domain

import org.springframework.stereotype.Component

@Component
class BookListParser {

    private val numberedPrefix = Regex("""^\s*\d+\s*[.)-]\s*""")

    fun parse(input: String): List<String> {
        require(input.isNotBlank()) {
            "Book list cannot be empty"
        }

        // If semicolons are present, use them as separators.
        // Otherwise, treat each non-empty line as one book.
        val rawBooks = if (';' in input) {
            input.split(';')
        } else {
            input.lines()
        }

        val books = rawBooks
            .map { line ->
                numberedPrefix.replace(line, "").trim()
            }
            .filter { it.isNotBlank() }

        require(books.size == 10) {
            "Expected exactly 10 books, but received ${books.size}"
        }

        val normalizedBooks = books.map {
            it.lowercase().replace(Regex("\\s+"), " ")
        }

        require(normalizedBooks.distinct().size == books.size) {
            "Duplicate books are not allowed"
        }

        return books
    }
}