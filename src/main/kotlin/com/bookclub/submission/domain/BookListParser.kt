package com.bookclub.submission.domain

import org.springframework.stereotype.Component

@Component
class BookListParser {

    data class ParsedBook(val title: String, val author: String? = null) {
        override fun toString(): String = author?.let { "$title — $it" } ?: title
    }

    private val numberedPrefix = Regex("""^\s*\d+\s*[.)-]\s*""")

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

        val parsed = books.map { line ->
            val parts = line.split(Regex("\\s+[—–-]\\s+"), limit = 2)
            ParsedBook(
                title = parts[0].trim(),
                author = parts.getOrNull(1)?.trim()?.takeIf { it.isNotBlank() }
            )
        }

        require(parsed.all { it.title.isNotBlank() }) { "Book titles cannot be blank" }

        val normalizedBooks = parsed.map {
            it.title.lowercase().replace(Regex("\\s+"), " ").trim()
        }

        require(normalizedBooks.distinct().size == books.size) {
            "Duplicate books are not allowed"
        }

        return parsed
    }

    private fun cleanLine(line: String): String = line
        .replace(Regex("^#{1,6}\\s*"), "")
        .replace("**", "")
        .trim()
}
