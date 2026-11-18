package com.bookclub.poll.application

import com.bookclub.infrastructure.telegram.TelegramBotService
import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import org.springframework.stereotype.Service

@Service
class PollClosingService(
    private val pollRounds: PollRoundRepository,
    private val pollResultService: PollResultService,
    private val pollRoundService: PollRoundService,
    private val telegramBotService: TelegramBotService,
    private val adminNotificationService: PollAdminNotificationService
) {

    fun closePoll(pollRoundId: Long) {
        val claimed = pollRounds.claimForClosing(pollRoundId)

        if (claimed == 0) {
            return
        }

        val round = pollRounds.findById(pollRoundId).orElseThrow {
            IllegalArgumentException("Poll round not found")
        }

        val chatId = round.telegramChatId
        val messageId = round.telegramMessageId
            ?: throw IllegalStateException("Telegram message ID is missing")

        val stopResponse = telegramBotService.stopPoll(chatId, messageId)

        if (!stopResponse.isOk) {
            println(
                "Failed to stop poll $pollRoundId: ${stopResponse.description()}"
            )
            return
        }

        val result = pollResultService.calculateResult(pollRoundId)

        when (result.status) {
            "WON" -> {
                val winner = result.winners.first()

                telegramBotService.sendMessage(
                    chatId, "🏆 Книга месяца выбрана!\n\n" + "📚 ${winner.bookTitle}\n"
                )
            }


            "TIE" -> {
                val runoff = pollRoundService.startRunoffRound(
                    previousPollRoundId = pollRoundId,
                    tiedBookIds = result.winners.map { it.bookId }
                )

                try {
                    val response = telegramBotService.sendPoll(
                        chatId,
                        "⚖️ Ничья! Голосуем ещё раз, выбери одну книгу:",
                        runoff.bookTitles
                    )

                    check(response.isOk) {
                        "Failed to publish runoff poll: ${response.description()}"
                    }

                    val message = requireNotNull(response.message())
                    val pollId = requireNotNull(message.poll()?.id())
                    val newMessageId = requireNotNull(message.messageId())

                    pollRoundService.saveTelegramPollInfo(
                        pollRoundId = runoff.pollRoundId,
                        telegramPollId = pollId,
                        telegramMessageId = newMessageId.toLong()
                    )
                } catch (exception: Exception) {
                    pollRoundService.markRoundForAdminDecision(
                        runoff.pollRoundId
                    )

                    adminNotificationService.notifyAdmins(
                        chatId,
                        """
            ⚠️ Не удалось запустить второй раунд голосования.

            Причина: ${exception.message}

            Нужен выбор книги администратором клуба.
            """.trimIndent()
                    )
                }
            }

            "ADMIN_DECISION" -> {
                adminNotificationService.notifyAdmins(
                    chatId, """
                        😄 Кажется, у нас слишком много хороших вариантов!

                        Голосование не выявило победителя. Давайте вместе решим, какую книгу будем читать в этот раз!
                    """.trimIndent()
                )
            }
        }
    }
}