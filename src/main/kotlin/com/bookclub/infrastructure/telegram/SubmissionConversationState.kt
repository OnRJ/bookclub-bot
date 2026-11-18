
package com.bookclub.infrastructure.telegram

import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentHashMap

data class PendingSubmission(
    val groupId: Long,
    val userId: Long,
    val username: String?,
    val firstName: String,
    val books: List<String>
)

@Component
class SubmissionConversationState {

    private val activeSubmissions =
        ConcurrentHashMap<Long, PendingSubmission>()

    fun start(userId: Long, groupId: Long) {
        activeSubmissions[userId] = PendingSubmission(
            groupId = groupId,
            userId = userId,
            username = null,
            firstName = "",
            books = emptyList()
        )
    }

    fun saveBooks(
        userId: Long,
        username: String?,
        firstName: String,
        books: List<String>
    ) {
        val current = activeSubmissions[userId] ?: return

        activeSubmissions[userId] = current.copy(
            username = username,
            firstName = firstName,
            books = books
        )
    }

    fun get(userId: Long): PendingSubmission? =
        activeSubmissions[userId]

    fun clear(userId: Long) {
        activeSubmissions.remove(userId)
    }

    fun isActive(userId: Long): Boolean =
        activeSubmissions.containsKey(userId)
}
