package com.bookclub.infrastructure.telegram

import com.bookclub.poll.application.PollRoundResult
import com.bookclub.poll.application.PollRoundService
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.InputPollOption
import com.pengrad.telegrambot.request.SendPoll
import com.pengrad.telegrambot.request.SendMessage
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.beans.factory.annotation.Value
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@Service
class TelegramPollPublisher(
    private val telegramBot: TelegramBot,
    private val pollRoundService: PollRoundService,
    @param:Value($$"${telegram.bot.username:}")
    private val botUsername: String
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

        publishCatalogButtonSafely(pollRound)
    }

    private fun publishCatalogButtonSafely(pollRound: PollRoundResult) {
        try {
            if (botUsername.isBlank()) {
                log.error("TELEGRAM_BOT_USERNAME is not configured")
                return
            }

            val catalogUrl =
                "https://t.me/$botUsername?startapp=books_${pollRound.pollRoundId}"

            val keyboard = InlineKeyboardMarkup(
                InlineKeyboardButton("Посмотреть книги")
                    .url(catalogUrl)
            )

            val response = telegramBot.execute(
                SendMessage(
                    pollRound.telegramChatId,
                    "📚В каталоге собраны книги с описаниями — загляните, выберите понравившиеся и проголосуйте!"
                ).replyMarkup(keyboard)
            )

            log.info(
                "CATALOG_BUTTON: Telegram response: {}, {}",
                response.isOk,
                response.description()
            )
        } catch (e: Exception) {
            log.error("CATALOG_BUTTON ERROR: ${e.javaClass.name}: ${e.message}", e)
        }
    }
}
