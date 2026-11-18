package com.bookclub.poll.application

import com.bookclub.club.infrastructure.persistence.ClubMemberRepository
import com.bookclub.infrastructure.telegram.TelegramBotService
import org.springframework.stereotype.Service

@Service
class PollAdminNotificationService(
    private val clubMembers: ClubMemberRepository,
    private val telegramBotService: TelegramBotService
) {

    fun notifyAdmins(chatId: Long, message: String) {
        // Сначала уведомляем группу.
        try {
            telegramBotService.sendMessageChecked(chatId, message)
        } catch (exception: Exception) {
            println(
                "Failed to notify group $chatId: ${exception.message}"
            )
        }

        val admins = clubMembers.findAllByClubTelegramChatIdAndRoleWithUser(
            chatId,
            "ADMIN"
        )

        if (admins.isEmpty()) {
            println("No admins found for club $chatId")
            return
        }

        // Ошибка личного сообщения одному админу
        // не должна мешать уведомлению остальных.
        admins.forEach { admin ->
            val adminTelegramId = admin.user.telegramUserId

            try {
                telegramBotService.sendMessageChecked(
                    adminTelegramId,
                    message
                )
            } catch (exception: Exception) {
                println(
                    "Failed to notify admin $adminTelegramId: " +
                            exception.message
                )
            }
        }
    }
}
