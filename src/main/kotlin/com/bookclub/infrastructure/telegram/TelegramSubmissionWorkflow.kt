package com.bookclub.infrastructure.telegram

import com.bookclub.poll.application.PollRoundService
import com.bookclub.poll.application.StartPollCommand
import com.bookclub.submission.application.SubmitBooksCommand
import com.bookclub.submission.application.SubmitBooksUseCase
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.model.request.InputPollOption
import com.pengrad.telegrambot.request.SendPoll
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class TelegramSubmissionWorkflow(
    private val telegramBot: TelegramBot,
    private val conversationState: SubmissionConversationState,
    private val submissionTelegramHandler: SubmissionTelegramHandler,
    private val submitBooksUseCase: SubmitBooksUseCase,
    private val pollRoundService: PollRoundService
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun confirm(userId: Long, privateChatId: Long?) {
        val pending = conversationState.get(userId)
        if (pending == null || pending.books.size != 10) {
            privateChatId?.let { submissionTelegramHandler.sendMessage(it, "Не удалось найти список книг. Начни подачу заново.") }
            return
        }

        try {
            val result = submitBooksUseCase.execute(
                SubmitBooksCommand(
                    pending.groupId,
                    pending.userId,
                    pending.username,
                    pending.firstName,
                    pending.books.map { it.title },
                    pending.books.map { it.author }
                )
            )
            val pollRound = pollRoundService.startFirstRound(
                StartPollCommand(result.submissionId, pending.groupId)
            )
            val response = telegramBot.execute(
                SendPoll(
                    pending.groupId,
                    "📚 Голосование за книги на ${result.readingMonth}. Выбери две книги:",
                    pollRound.bookTitles.map(::InputPollOption)
                ).isAnonymous(false).allowsMultipleAnswers(true)
            )
            check(response.isOk) { "Telegram не смог опубликовать опрос: ${response.description()}" }
            val message = requireNotNull(response.message()) { "Telegram не вернул сообщение с опросом" }
            val telegramPollId = requireNotNull(message.poll()?.id()) { "Telegram не вернул ID опроса" }
            val telegramMessageId = requireNotNull(message.messageId()) { "Telegram не вернул ID сообщения" }
            pollRoundService.saveTelegramPollInfo(pollRound.pollRoundId, telegramPollId, telegramMessageId.toLong())
            conversationState.clear(userId)
            privateChatId?.let { submissionTelegramHandler.sendMessage(it, "✅ Список сохранён, голосование опубликовано в группе!") }
        } catch (exception: Exception) {
            log.error("Failed to complete submission workflow for user {}", userId, exception)
            privateChatId?.let { submissionTelegramHandler.sendMessage(it, "Не удалось завершить отправку: ${exception.message}") }
        }
    }
}
