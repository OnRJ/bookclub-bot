package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.model.CallbackQuery
import org.springframework.stereotype.Component

@Component
class TelegramCallbackHandler(
    private val conversationState: SubmissionConversationState,
    private val submissionWorkflow: TelegramSubmissionWorkflow,
    private val submissionTelegramHandler: SubmissionTelegramHandler
) {
    fun handle(callback: CallbackQuery) {
        val userId = callback.from().id()
        val chatId = callback.maybeInaccessibleMessage()?.chat()?.id()
        when (callback.data()) {
            "confirm_submission" -> submissionWorkflow.confirm(userId, chatId)
            "edit_submission" -> if (conversationState.get(userId) != null) {
                chatId?.let(submissionTelegramHandler::askForBooks)
            }
            "cancel_submission" -> {
                conversationState.clear(userId)
                chatId?.let { submissionTelegramHandler.sendMessage(it, "❌ Подача книг отменена.") }
            }
        }
    }
}
