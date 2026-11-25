
package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InputPollOption
import com.pengrad.telegrambot.request.GetMe
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPoll
import com.pengrad.telegrambot.request.StopPoll
import com.bookclub.poll.application.TelegramGateway
import com.bookclub.poll.application.TelegramPollPublication
import org.springframework.stereotype.Service

@Service
class TelegramBotService(
    private val telegramBot: TelegramBot
) : TelegramGateway {

    fun getBotInfo() = telegramBot.execute(GetMe())

    override fun sendMessage(chatId: Long, text: String) {
        telegramBot.execute(SendMessage(chatId, text))
    }

    override fun sendPoll(
        chatId: Long,
        question: String,
        options: List<String>
    ): TelegramPollPublication {
        val response = telegramBot.execute(
        SendPoll(
            chatId,
            question,
            options.map { InputPollOption(it) }
        )
            .isAnonymous(false)
            .allowsMultipleAnswers(true)
        )
        return TelegramPollPublication(
            response.isOk,
            response.description(),
            response.message()?.poll()?.id(),
            response.message()?.messageId()?.toLong()
        )
    }

    override fun stopPoll(chatId: Long, messageId: Long): TelegramPollPublication {
        val response = telegramBot.execute(StopPoll(chatId, messageId.toInt()))
        return TelegramPollPublication(response.isOk, response.description())
    }

    override fun sendMessageChecked(chatId: Long, text: String) {
        val response = telegramBot.execute(SendMessage(chatId, text))

        check(response.isOk) {
            "Telegram sendMessage failed: ${response.description()}"
        }
    }
}
