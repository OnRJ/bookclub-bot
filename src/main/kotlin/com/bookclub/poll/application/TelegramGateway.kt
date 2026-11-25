package com.bookclub.poll.application

data class TelegramPollPublication(
    val ok: Boolean,
    val description: String?,
    val pollId: String? = null,
    val messageId: Long? = null
)

interface TelegramGateway {
    fun sendMessage(chatId: Long, text: String)
    fun sendMessageChecked(chatId: Long, text: String)
    fun sendPoll(chatId: Long, question: String, options: List<String>): TelegramPollPublication
    fun stopPoll(chatId: Long, messageId: Long): TelegramPollPublication
}
