
package com.bookclub.infrastructure.telegram

import com.bookclub.submission.domain.BookListParser
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import org.springframework.stereotype.Component

@Component
class SubmissionTelegramHandler(
    private val telegramBot: TelegramBot,
    private val bookListParser: BookListParser,
    private val conversationState: SubmissionConversationState
) {

    fun handle(
        chatId: Long,
        userId: Long,
        username: String?,
        firstName: String,
        text: String
    ) {
        val booksText = text.replaceFirst(
            Regex("""^\s*/submit_books(?:@\w+)?(?:\s+|$)"""),
            ""
        ).trim()

        if (booksText.isBlank()) {
            sendMessage(
                chatId,
                "📚 Отправь одним сообщением список ровно из 10 книг."
            )
            return
        }

        try {
            val books = bookListParser.parse(booksText)

            conversationState.saveBooks(
                userId = userId,
                username = username,
                firstName = firstName,
                books = books
            )

            val bookList = books.mapIndexed { index, book ->
                "${index + 1}) $book"
            }.joinToString("\n")

            val message = """
                📚 Твой список книг:

                $bookList

                Всего: ${books.size}/10

                Всё правильно?
            """.trimIndent()

            val keyboard = InlineKeyboardMarkup(
                arrayOf(
                    InlineKeyboardButton("✅ Confirm")
                        .callbackData("confirm_submission"),
                    InlineKeyboardButton("✏️ Edit")
                        .callbackData("edit_submission")
                ),
                arrayOf(
                    InlineKeyboardButton("❌ Cancel")
                        .callbackData("cancel_submission")
                )
            )

            telegramBot.execute(
                SendMessage(chatId, message).replyMarkup(keyboard)
            )

        } catch (exception: IllegalArgumentException) {
            sendMessage(
                chatId,
                """
                Не удалось распознать список книг.

                Причина: ${exception.message}

                Попробуй отправить ровно 10 книг:
                • с новой строки;
                • с нумерацией;
                • через точку с запятой (;).
                """.trimIndent()
            )
        }
    }

    fun askForBooks(chatId: Long) {
        sendMessage(
            chatId,
            "✏️ Отправь исправленный список из 10 книг одним сообщением."
        )
    }

    fun sendMessage(chatId: Long, text: String) {
        telegramBot.execute(SendMessage(chatId, text))
    }
}
