package com.bookclub.infrastructure.telegram

import com.bookclub.club.application.ClubMemberRegistrationService
import com.bookclub.club.application.ClubRegistrationService
import com.pengrad.telegrambot.model.Update
import org.springframework.stereotype.Component

@Component
class TelegramMembershipUpdateHandler(
    private val clubs: ClubRegistrationService,
    private val members: ClubMemberRegistrationService
) {
    fun handle(update: Update): Boolean {
        val change = update.myChatMember() ?: return false
        val status = change.newChatMember().status()
        if (status == com.pengrad.telegrambot.model.ChatMember.Status.member ||
            status == com.pengrad.telegrambot.model.ChatMember.Status.administrator) {
            clubs.registerOrActivate(change.chat().id(), change.chat().title() ?: "Book Club")
        }
        return true
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
            members.registerMember(message.chat().id(), member.id(), member.username(), member.firstName() ?: "Unknown")
        }
    }

    fun registerSender(chatId: Long, userId: Long, username: String?, firstName: String) {
        if (chatId < 0) {
            clubs.registerOrActivate(chatId, "Book Club")
            members.registerMember(chatId, userId, username, firstName)
        }
    }
}
