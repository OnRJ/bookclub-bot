package com.bookclub.poll.application

import com.bookclub.poll.domain.PollRoundStatus

import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import org.springframework.stereotype.Service
import org.slf4j.LoggerFactory

@Service
class PollClosingService(
    private val pollRounds: PollRoundRepository,
    private val pollResultService: PollResultService,
    private val pollRoundService: PollRoundService,
    private val telegramGateway: TelegramGateway,
    private val adminNotificationService: PollAdminNotificationService
) {
    private val log = LoggerFactory.getLogger(javaClass)

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

        val stopResponse = telegramGateway.stopPoll(chatId, messageId)

        if (!stopResponse.ok) {
            log.error("Failed to stop poll {}: {}", pollRoundId, stopResponse.description)
            return
        }

        val result = pollResultService.calculateResult(pollRoundId)

        when (result.status) {
            PollRoundStatus.WON -> {
                val winner = result.winners.first()

                telegramGateway.sendMessage(
                    chatId, "🏆 Книга месяца выбрана!\n\n" + "📚 ${winner.bookTitle}\n"
                )
            }


            PollRoundStatus.TIE -> {
                val runoff = pollRoundService.startRunoffRound(
                    previousPollRoundId = pollRoundId,
                    tiedBookIds = result.winners.map { it.bookId }
                )

                try {
                    val response = telegramGateway.sendPoll(
                        chatId,
                        "⚖️ Ничья! Голосуем ещё раз, выбери одну книгу:",
                        runoff.bookTitles
                    )

                    check(response.ok) {
                        "Failed to publish runoff poll: ${response.description}"
                    }

                    val pollId = requireNotNull(response.pollId)
                    val newMessageId = requireNotNull(response.messageId)

                    pollRoundService.saveTelegramPollInfo(
                        pollRoundId = runoff.pollRoundId,
                        telegramPollId = pollId,
                        telegramMessageId = newMessageId
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

            PollRoundStatus.ADMIN_DECISION -> {
                adminNotificationService.notifyAdmins(
                    chatId, """
                        😄 Кажется, у нас слишком много хороших вариантов!

                        Голосование не выявило победителя. Давайте вместе решим, какую книгу будем читать в этот раз!
                    """.trimIndent()
                )
            }

            else -> error("Unexpected poll result status: ${result.status}")
        }
    }
}
