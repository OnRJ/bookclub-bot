package com.bookclub.submission.application

import com.bookclub.submission.domain.ReadingMonthPolicy
import com.bookclub.submission.domain.BookListParser.ParsedBook
import org.springframework.stereotype.Service
import com.bookclub.shared.application.ValidationException
import java.time.Clock
import java.time.LocalDate

@Service
class SubmitBooksUseCase(
    private val writer: SubmissionWriter,
    private val monthPolicy: ReadingMonthPolicy, private val clock: Clock
) {
    fun execute(command: SubmitBooksCommand): SubmissionResult {
        if (command.telegramChatId >= 0) throw ValidationException("Must be called from a group")
        if (command.telegramUserId <= 0) throw ValidationException("Invalid Telegram user ID")
        if (command.firstName.isBlank()) throw ValidationException("First name is required")
        if (command.bookTitles.size != 10) throw ValidationException("Exactly 10 titles are required")
        if (command.bookTitles.any { it.isBlank() }) throw ValidationException("Titles cannot be blank")
        if (command.bookTitles.map { it.lowercase().trim() }.distinct().size != 10) throw ValidationException("Duplicate titles")
        val books = command.bookTitles.mapIndexed { index, title ->
            ParsedBook(title.trim(), command.bookAuthors.getOrNull(index))
        }
        val month = monthPolicy.determine(LocalDate.now(clock))
        return writer.create(
            command.telegramChatId, command.telegramUserId,
            command.username, command.firstName, month, books
        )
    }
}
