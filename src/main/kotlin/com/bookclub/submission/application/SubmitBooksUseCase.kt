package com.bookclub.submission.application

import com.bookclub.submission.domain.ReadingMonthPolicy
import org.springframework.stereotype.Service
import java.time.Clock
import java.time.LocalDate

@Service
class SubmitBooksUseCase(
    private val writer: SubmissionWriter, private val clubs: ClubLookup,
    private val monthPolicy: ReadingMonthPolicy, private val clock: Clock
) {
    fun execute(command: SubmitBooksCommand): SubmissionResult {
        require(command.telegramChatId < 0) { "Must be called from a group" }
        require(command.telegramUserId > 0) { "Invalid Telegram user ID" }
        require(command.firstName.isNotBlank()) { "First name is required" }
        require(command.bookTitles.size == 10) { "Exactly 10 titles are required" }
        require(command.bookTitles.all { it.isNotBlank() }) { "Titles cannot be blank" }
        val titles = command.bookTitles.map(String::trim)
        require(titles.map { it.lowercase() }.distinct().size == 10) { "Duplicate titles" }
        val zone = clubs.findTimezoneByTelegramChatId(command.telegramChatId)
            ?: throw IllegalArgumentException("Unregistered club")
        val month = monthPolicy.determine(LocalDate.now(clock.withZone(zone)))
        return writer.create(
            command.telegramChatId, command.telegramUserId,
            command.username, command.firstName, month, titles
        )
    }
}
