package com.bookclub.poll.application

import com.bookclub.club.infrastructure.persistence.ClubMemberRepository
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class PollAdminNotificationService(
    private val clubMembers: ClubMemberRepository,
    private val telegramGateway: TelegramGateway
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun notifyAdmins(chatId: Long, message: String) {
        // Сначала уведомляем группу.
        try {
            telegramGateway.sendMessageChecked(chatId, message)
        } catch (exception: Exception) {
            log.warn("Failed to notify group {}: {}", chatId, exception.message, exception)
        }

        val admins = clubMembers.findAllByClubTelegramChatIdAndRoleWithUser(
            chatId,
            "ADMIN"
        )

        if (admins.isEmpty()) {
            log.info("No admins found for club {}", chatId)
            return
        }

        // Ошибка личного сообщения одному админу
        // не должна мешать уведомлению остальных.
        admins.forEach { admin ->
            val adminTelegramId = admin.user.telegramUserId

            try {
                    telegramGateway.sendMessageChecked(
                    adminTelegramId,
                    message
                )
            } catch (exception: Exception) {
                log.warn("Failed to notify admin {}: {}", adminTelegramId, exception.message, exception)
            }
        }
    }
}
