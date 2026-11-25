package com.bookclub.infrastructure.telegram

import com.bookclub.club.application.ClubMemberRegistrationService
import com.bookclub.club.application.ClubRegistrationService
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.request.SendMessage
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TelegramMembershipUpdateHandler(
    private val clubs: ClubRegistrationService,
    private val members: ClubMemberRegistrationService,
    private val telegramBot: TelegramBot,
    @param:Value("\${telegram.bot.username}") private val botUsername: String
) {
    fun handle(update: Update): Boolean {
        val change = update.myChatMember() ?: return false
        val oldStatus = change.oldChatMember().status()
        val newStatus = change.newChatMember().status()

        val isActive =
            newStatus == com.pengrad.telegrambot.model.ChatMember.Status.member ||
                    newStatus == com.pengrad.telegrambot.model.ChatMember.Status.administrator

        val wasActive =
            oldStatus == com.pengrad.telegrambot.model.ChatMember.Status.member ||
                    oldStatus == com.pengrad.telegrambot.model.ChatMember.Status.administrator

        if (isActive) {
            val chat = change.chat()
            val chatId = chat.id()

            clubs.registerOrActivate(chatId, chat.title() ?: "Book Club")

            if (!wasActive) {
                sendWelcome(chatId)
            }
        }

        return true
    }

    private fun sendWelcome(chatId: Long) {
        val keyboard = InlineKeyboardMarkup(
            InlineKeyboardButton("📚 Предложить список книг")
                .url("https://t.me/$botUsername?startapp=club_$chatId")
        )

        val text = """
            📚 BookWormClub

            Привет! Я BookWormClub — помощник вашего книжного клуба.

            Я помогу собирать предложения книг и организовывать голосования, чтобы вместе выбирать, что читать дальше.

            Хотите предложить книги для будущего голосования?
        """.trimIndent()

        telegramBot.execute(
            SendMessage(chatId, text).replyMarkup(keyboard)
        )
    }

    fun registerNewMembers(update: Update) {
        val message = update.message() ?: return

        if (message.chat().id() < 0) {
            clubs.registerOrActivate(
                message.chat().id(),
                message.chat().title() ?: "Book Club"
            )
        }

        message.newChatMembers()?.forEach { member ->
            if (member.isBot) return@forEach

            members.registerMember(
                message.chat().id(),
                member.id(),
                member.username(),
                member.firstName() ?: "Unknown"
            )
        }
    }

    fun registerSender(
        chatId: Long,
        userId: Long,
        username: String?,
        firstName: String
    ) {
        if (chatId < 0) {
            clubs.registerOrActivate(chatId, "Book Club")
            members.registerMember(chatId, userId, username, firstName)
        }
    }
}