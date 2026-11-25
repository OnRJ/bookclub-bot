
package com.bookclub.infrastructure.telegram

import com.bookclub.poll.application.PollRoundResult
import com.bookclub.poll.application.PollRoundService
import com.bookclub.infrastructure.bookinfo.GoogleBooksService
import com.bookclub.infrastructure.bookinfo.BookInfoFormatter
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InputPollOption
import com.pengrad.telegrambot.request.SendPoll
import com.pengrad.telegrambot.request.SendMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Service
class TelegramPollPublisher(
    private val telegramBot: TelegramBot,
    private val pollRoundService: PollRoundService,
    private val googleBooksService: GoogleBooksService,
    private val bookInfoFormatter: BookInfoFormatter
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun publishFirstRound(
        pollRound: PollRoundResult,
        readingMonth: YearMonth
    ) {
        val pollOptions = pollRound.bookTitles.map { book ->
            InputPollOption(book)
        }

        val monthName = readingMonth.month
            .getDisplayName(TextStyle.FULL, Locale.forLanguageTag("ru"))
            .replaceFirstChar { it.titlecase(Locale.forLanguageTag("ru")) }

        val response = telegramBot.execute(
            SendPoll(
                pollRound.telegramChatId,
                "📚 Голосование за книгу $monthName. Выбери две книги:",
                pollOptions
            )
                .isAnonymous(false)
                .allowsMultipleAnswers(true)
        )

        check(response.isOk) {
            "Telegram не смог опубликовать опрос: ${response.description()}"
        }

        val message = requireNotNull(response.message()) {
            "Telegram не вернул сообщение с опросом"
        }

        val telegramPollId = requireNotNull(message.poll()?.id()) {
            "Telegram не вернул ID опроса"
        }

        val telegramMessageId = requireNotNull(message.messageId()) {
            "Telegram не вернул ID сообщения"
        }

        pollRoundService.saveTelegramPollInfo(
            pollRoundId = pollRound.pollRoundId,
            telegramPollId = telegramPollId,
            telegramMessageId = telegramMessageId.toLong()
        )

        publishBookInfoSafely(pollRound)
    }

    private fun publishBookInfoSafely(pollRound: PollRoundResult) {
        try {
            val books = pollRound.bookTitles.mapNotNull { title ->
                googleBooksService.search(title)
            }

            if (books.isEmpty()) return

            val text = bookInfoFormatter.format(books)

            telegramBot.execute(
                SendMessage(pollRound.telegramChatId, text)
            )
        } catch (e: Exception) {
            log.error("Failed to publish book information: ${e.message}")
        }
    }
}
