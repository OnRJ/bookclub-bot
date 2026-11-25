package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.Update
import org.springframework.stereotype.Component

@Component
class TelegramUpdateHandler(
    private val conversationState: SubmissionConversationState,
    private val submissionTelegramHandler: SubmissionTelegramHandler,
    private val membershipHandler: TelegramMembershipUpdateHandler,
    private val pollAnswerHandler: TelegramPollAnswerHandler,
    private val commandHandler: TelegramCommandHandler,
    private val callbackHandler: TelegramCallbackHandler
) : UpdatesListener {

    override fun process(updates: List<Update>): Int {
        updates.forEach { update ->

            if (membershipHandler.handle(update)) {
                return@forEach
            }
            membershipHandler.registerNewMembers(update)

            val pollAnswer = update.pollAnswer()

            if (pollAnswer != null) {
                pollAnswerHandler.handle(pollAnswer)
                return@forEach
            }

            val callback = update.callbackQuery()

            if (callback != null) {
                callbackHandler.handle(callback)
                return@forEach
            }

            val message = update.message() ?: return@forEach
            val text = message.text() ?: return@forEach

            val chatId = message.chat().id()
            val sender = message.from() ?: return@forEach
            val userId = sender.id()

            if (chatId < 0) {
                membershipHandler.registerSender(chatId, userId, sender.username(), sender.firstName() ?: "Unknown")
            }

            if (commandHandler.handle(chatId, userId, sender.username(), sender.firstName(), text)) {
                return@forEach
            }

            if (chatId > 0 && conversationState.isActive(userId)) {
                submissionTelegramHandler.handle(
                    chatId,
                    userId,
                    sender.username(),
                    sender.firstName(),
                    text
                )
            }
        }

        return UpdatesListener.CONFIRMED_UPDATES_ALL
    }

}
