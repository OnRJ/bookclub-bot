package com.bookclub.infrastructure.telegram

import com.bookclub.poll.application.PollAnswerService
import com.bookclub.poll.application.PollRoundService
import com.bookclub.poll.application.StartPollCommand
import com.bookclub.submission.application.SubmitBooksCommand
import com.bookclub.submission.application.SubmitBooksUseCase
import com.pengrad.telegrambot.TelegramBot
import com.pengrad.telegrambot.UpdatesListener
import com.pengrad.telegrambot.model.PollAnswer
import com.pengrad.telegrambot.model.Update
import com.pengrad.telegrambot.model.request.InlineKeyboardButton
import com.pengrad.telegrambot.model.request.InlineKeyboardMarkup
import com.pengrad.telegrambot.model.request.InputPollOption
import com.pengrad.telegrambot.request.SendMessage
import com.pengrad.telegrambot.request.SendPoll
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class TelegramUpdateHandler(
    private val telegramBot: TelegramBot,
    @param:Value($$"${telegram.bot.username}")
    private val botUsername: String,
    private val conversationState: SubmissionConversationState,
    private val submissionTelegramHandler: SubmissionTelegramHandler,
    private val submitBooksUseCase: SubmitBooksUseCase,
    private val pollRoundService: PollRoundService,
    private val pollAnswerService: PollAnswerService
) : UpdatesListener {

    override fun process(updates: List<Update>): Int {
        updates.forEach { update ->

            val pollAnswer = update.pollAnswer()

            if (pollAnswer != null) {
                handlePollAnswer(pollAnswer)
                return@forEach
            }

            val callback = update.callbackQuery()

            if (callback != null) {
                handleCallback(callback)
                return@forEach
            }

            val message = update.message() ?: return@forEach
            val text = message.text() ?: return@forEach

            val chatId = message.chat().id()
            val sender = message.from() ?: return@forEach
            val userId = sender.id()

            val trimmedText = text.trim()
            val parts = trimmedText.split(Regex("""\s+"""), limit = 2)
            val command = parts.first().substringBefore("@")
            val argument = parts.getOrNull(1)

            if (trimmedText.startsWith("/")) {
                when (command) {
                    "/chat_id" -> {
                        telegramBot.execute(
                            SendMessage(
                                chatId,
                                "ID этого чата: $chatId"
                            )
                        )
                    }
                    "/start" -> handleStart(chatId, userId, argument)
                    "/help" -> handleHelp(chatId)
                    "/submit_books" -> {
                        if (chatId < 0) {
                            handleSubmitBooks(chatId)
                        } else {
                            submissionTelegramHandler.handle(
                                chatId,
                                userId,
                                sender.username(),
                                sender.firstName(),
                                trimmedText
                            )
                        }
                    }
                    else -> handleUnknownCommand(chatId)
                }

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

    private fun handleCallback(
        callback: com.pengrad.telegrambot.model.CallbackQuery
    ) {
        val userId = callback.from().id()
        val chatId = callback.maybeInaccessibleMessage()?.chat()?.id()

        when (callback.data()) {
            "confirm_submission" -> confirmSubmission(userId, chatId)
            "edit_submission" -> {
                if (conversationState.get(userId) != null) {
                    submissionTelegramHandler.askForBooks(chatId ?: return)
                }
            }
            "cancel_submission" -> {
                conversationState.clear(userId)
                if (chatId != null) {
                    submissionTelegramHandler.sendMessage(
                        chatId,
                        "❌ Подача книг отменена."
                    )
                }
            }
        }
    }

    private fun confirmSubmission(userId: Long, privateChatId: Long?) {
        val pending = conversationState.get(userId)

        if (pending == null || pending.books.size != 10) {
            if (privateChatId != null) {
                submissionTelegramHandler.sendMessage(
                    privateChatId,
                    "Не удалось найти список книг. Начни подачу заново."
                )
            }
            return
        }

        try {
            // 1. Сохраняем заявку
            val result = submitBooksUseCase.execute(
                SubmitBooksCommand(
                    telegramChatId = pending.groupId,
                    telegramUserId = pending.userId,
                    username = pending.username,
                    firstName = pending.firstName,
                    bookTitles = pending.books
                )
            )

            // 2. Создаём первый раунд голосования и сохраняем его в БД
            val pollRound = pollRoundService.startFirstRound(
                StartPollCommand(
                    submissionId = result.submissionId,
                    telegramChatId = pending.groupId
                )
            )

            // 3. Формируем варианты из сохранённого раунда
            val pollOptions = pollRound.bookTitles.map { book ->
                InputPollOption(book)
            }

            // 4. Публикуем Telegram Poll
            val poll = SendPoll(
                pending.groupId,
                "📚 Голосование за книги на ${result.readingMonth}. Выбери две книги:",
                pollOptions
            )
                .isAnonymous(false)
                .allowsMultipleAnswers(true)

            val response = telegramBot.execute(poll)

            if (!response.isOk) {
                throw IllegalStateException(
                    "Telegram не смог опубликовать опрос: ${response.description()}"
                )
            }

            // 5. Получаем данные опубликованного опроса
            val message = requireNotNull(response.message()) {
                "Telegram не вернул сообщение с опросом"
            }

            val telegramPollId = requireNotNull(message.poll()?.id()) {
                "Telegram не вернул ID опроса"
            }

            val telegramMessageId = requireNotNull(message.messageId()) {
                "Telegram не вернул ID сообщения"
            }

            // 6. Сохраняем Telegram ID в poll_rounds
            pollRoundService.saveTelegramPollInfo(
                pollRoundId = pollRound.pollRoundId,
                telegramPollId = telegramPollId,
                telegramMessageId = telegramMessageId.toLong()
            )

            conversationState.clear(userId)

            if (privateChatId != null) {
                submissionTelegramHandler.sendMessage(
                    privateChatId,
                    "✅ Список сохранён, голосование опубликовано в группе!"
                )
            }

        } catch (exception: Exception) {
            if (privateChatId != null) {
                submissionTelegramHandler.sendMessage(
                    privateChatId,
                    "Не удалось завершить отправку: ${exception.message}"
                )
            }
        }
    }

    private fun handleStart(
        chatId: Long,
        userId: Long,
        argument: String?
    ) {
        if (argument?.startsWith("submit_") == true) {
            if (chatId <= 0) return

            val groupId = argument
                .removePrefix("submit_")
                .toLongOrNull()

            if (groupId == null || groupId >= 0) {
                submissionTelegramHandler.sendMessage(
                    chatId,
                    "Некорректная ссылка на подачу книг."
                )
                return
            }

            conversationState.start(userId, groupId)

            submissionTelegramHandler.sendMessage(
                chatId,
                """
                Привет! 📚

                Отправь одним сообщением список из 10 книг.

                Поддерживаемые форматы:
                • Нумерованный список
                • Каждая книга с новой строки
                • Названия через точку с запятой (;)
                """.trimIndent()
            )
            return
        }

        submissionTelegramHandler.sendMessage(
            chatId,
            """
            Привет! 📚 Я BookWormClubBot.

            Я помогу нашему книжному клубу выбирать книги месяца!

            Используй /help для просмотра команд.
            """.trimIndent()
        )
    }

    private fun handleSubmitBooks(chatId: Long) {
        val miniAppUrl =
            "https://t.me/$botUsername?startapp=club_$chatId"

        val keyboard = InlineKeyboardMarkup(
            InlineKeyboardButton("Предложить список книг")
                .url(miniAppUrl)
        )

        telegramBot.execute(
            SendMessage(
                chatId,
                "📚 Готов выбрать книги для следующего месяца?"
            ).replyMarkup(keyboard)
        )
    }

    private fun handleHelp(chatId: Long) {
        submissionTelegramHandler.sendMessage(
            chatId,
            """
            📚 BookWormClubBot — команды

            /start — начать работу
            /help — список команд
            /submit_books — предложить 10 книг
            """.trimIndent()
        )
    }

    private fun handlePollAnswer(pollAnswer: PollAnswer) {
        val userId = pollAnswer.user()?.id() ?: return
        val pollId = pollAnswer.pollId()
        val optionIds = pollAnswer.optionIds()?.toList() ?: emptyList()

        pollAnswerService.saveAnswerAndCheckCompletion(
            telegramPollId = pollId,
            telegramUserId = userId,
            selectedOptionIds = optionIds
        )
    }

    private fun handleUnknownCommand(chatId: Long) {
        submissionTelegramHandler.sendMessage(
            chatId,
            "Не совсем понял тебя 🤔\nИспользуй /help."
        )
    }
}
