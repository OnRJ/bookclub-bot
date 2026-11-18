
package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InputPollOption
import com.pengrad.telegrambot.request.GetMe
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPoll
import com.pengrad.telegrambot.request.StopPoll
import org.springframework.stereotype.Service

@Service
class TelegramBotService(
    private val telegramBot: TelegramBot
) {

    fun getBotInfo() = telegramBot.execute(GetMe())

    fun sendMessage(chatId: Long, text: String) =
        telegramBot.execute(SendMessage(chatId, text))

    fun sendPoll(
        chatId: Long,
        question: String,
        options: List<String>
    ) = telegramBot.execute(
        SendPoll(
            chatId,
            question,
            options.map { InputPollOption(it) }
        )
            .isAnonymous(false)
            .allowsMultipleAnswers(true)
    )

    fun stopPoll(chatId: Long, messageId: Long) =
        telegramBot.execute(
            StopPoll(chatId, messageId.toInt())
        )

    fun sendMessageChecked(chatId: Long, text: String) {
        val response = sendMessage(chatId, text)

        check(response.isOk) {
            "Telegram sendMessage failed: ${response.description()}"
        }
    }
}
