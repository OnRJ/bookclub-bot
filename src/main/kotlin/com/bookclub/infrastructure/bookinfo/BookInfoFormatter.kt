package com.bookclub.infrastructure.bookinfo

import org.springframework.stereotype.Service

@Service
class BookInfoFormatter {

    fun format(books: List<BookInfo>): String {
        val sections = books.mapIndexed { index, book ->
            buildString {
                append("${index + 1}. 📚 ${book.title}")

                book.author?.let {
                    append("\n✍️ $it")
                }

                book.description?.let {
                    append("\n\n$it")
                }

                book.pageCount?.let {
                    append("\n\n📖 $it стр.")
                }
            }
        }

        return sections.joinToString("\n\n──────────\n\n")
    }
}
