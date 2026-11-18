package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.ChatMember
import com.pengrad.telegrambot.request.GetChatMember
import org.springframework.stereotype.Component

@Component
class TelegramClubMembershipValidator(
    private val telegramBot: TelegramBot
) {

    fun isMember(
        telegramChatId: Long,
        telegramUserId: Long
    ): Boolean {
        val response = telegramBot.execute(
            GetChatMember(telegramChatId, telegramUserId)
        )

        check(response.isOk) {
            "Unable to verify Telegram club membership: ${response.description()}"
        }

        val member = response.chatMember()
            ?: throw IllegalStateException(
                "Telegram returned an empty chat member"
            )

        return when (member.status()) {
            ChatMember.Status.creator,
            ChatMember.Status.administrator,
            ChatMember.Status.member -> true

            ChatMember.Status.restricted -> member.isMember() == true

            ChatMember.Status.left,
            ChatMember.Status.kicked -> false

            else -> false
        }
    }
}
