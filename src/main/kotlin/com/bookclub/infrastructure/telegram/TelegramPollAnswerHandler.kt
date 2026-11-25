package com.bookclub.infrastructure.telegram

import com.bookclub.poll.application.PollAnswerService
import com.pengrad.telegrambot.model.PollAnswer
import org.springframework.stereotype.Component

@Component
class TelegramPollAnswerHandler(private val polls: PollAnswerService) {
    fun handle(answer: PollAnswer) {
        val userId = answer.user()?.id() ?: return
        polls.saveAnswerAndCheckCompletion(answer.pollId(), userId, answer.optionIds()?.toList() ?: emptyList())
    }
}
