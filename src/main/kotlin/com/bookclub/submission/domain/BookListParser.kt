package com.bookclub.submission.domain

import org.springframework.stereotype.Component

@Component
class BookListParser {

    data class ParsedBook(val title: String, val author: String? = null) {
        override fun toString(): String = author?.let { "$title — $it" } ?: title
    }

    // Avoid treating numeric titles such as "1984 - Orwell" as list markers.
    private val numberedPrefix = Regex("""^\s*\d{1,2}\s*[.)-]\s*""")

    fun parse(input: String): List<ParsedBook> {
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
                cleanLine(numberedPrefix.replace(line, "").trim())
            }
            .filter { it.isNotBlank() }

        require(books.size == 10) {
            "Expected exactly 10 books, but received ${books.size}"
        }

        val parsed = books.map(::parseBookLine)

        require(parsed.all { it.title.isNotBlank() }) { "Book titles cannot be blank" }

        val normalizedBooks = parsed.map {
            it.title.lowercase().replace(Regex("\\s+"), " ").trim()
        }

        require(normalizedBooks.distinct().size == books.size) {
            "Duplicate books are not allowed"
        }

        return parsed
    }

    fun parseBookLine(line: String): ParsedBook {
        val cleaned = cleanLine(numberedPrefix.replace(line, "").trim())
        val parts = cleaned.split(Regex("\\s*[—–]\\s*|\\s+-\\s+"), limit = 2)
        return ParsedBook(
            title = parts[0].trim(),
            author = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
        )
    }

    private fun cleanLine(line: String): String = line
        .replace(Regex("^#{1,6}\\s*"), "")
        .replace("**", "")
        .trim()
}
