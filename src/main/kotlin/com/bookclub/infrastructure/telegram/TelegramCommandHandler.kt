package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TelegramCommandHandler(
    private val telegramBot: TelegramBot,
    @param:Value($$"${telegram.bot.username}") private val botUsername: String,
    private val conversationState: SubmissionConversationState,
    private val submissionTelegramHandler: SubmissionTelegramHandler
) {
    fun handle(
        chatId: Long,
        userId: Long,
        username: String?,
        firstName: String?,
        text: String
    ): Boolean {
        val trimmed = text.trim()
        if (!trimmed.startsWith("/")) return false
        val parts = trimmed.split(Regex("\\s+"), limit = 2)
        val command = parts.first().substringBefore("@")
        val argument = parts.getOrNull(1)

        when (command) {
            "/chat_id" -> telegramBot.execute(SendMessage(chatId, "ID этого чата: $chatId"))
            "/start" -> start(chatId, userId, argument)
            "/help" -> help(chatId)
            "/submit_books" -> if (chatId < 0) submitBooks(chatId) else
                submissionTelegramHandler.handle(chatId, userId, username, firstName ?: "Unknown", trimmed)
            else -> submissionTelegramHandler.sendMessage(chatId, "Не совсем понял тебя 🤔\nИспользуй /help.")
        }
        return true
    }

    private fun start(chatId: Long, userId: Long, argument: String?) {
        if (argument?.startsWith("submit_") == true) {
            if (chatId <= 0) return
            val groupId = argument.removePrefix("submit_").toLongOrNull()
            if (groupId == null || groupId >= 0) {
                submissionTelegramHandler.sendMessage(chatId, "Некорректная ссылка на подачу книг.")
                return
            }
            conversationState.start(userId, groupId)
            submissionTelegramHandler.sendMessage(chatId, "Привет! 📚\n\nОтправь одним сообщением список из 10 книг.")
            return
        }
        submissionTelegramHandler.sendMessage(chatId, "Привет! 📚 Я BookWormClubBot.\n\nИспользуй /help для просмотра команд.")
    }

    private fun submitBooks(chatId: Long) {
        val keyboard = InlineKeyboardMarkup(
            InlineKeyboardButton("Предложить список книг").url("https://t.me/$botUsername?startapp=club_$chatId")
        )
        telegramBot.execute(SendMessage(chatId, "📖 Какую книгу будем читать следующей?").replyMarkup(keyboard))
    }

    private fun help(chatId: Long) = submissionTelegramHandler.sendMessage(
        chatId, "📚 BookWormClubBot — команды\n\n/start — начать работу\n/help — список команд\n/submit_books — предложить 10 книг"
    )
}
