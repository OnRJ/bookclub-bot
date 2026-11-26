package com.bookclub.poll.api

import com.bookclub.poll.infrastructure.persistence.PollRoundRepository
import com.bookclub.poll.infrastructure.persistence.PollRoundOptionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
class BookCatalogService(
    private val pollRoundRepository: PollRoundRepository,
    private val pollRoundOptionRepository: PollRoundOptionRepository
) {

    @Transactional(readOnly = true)
    fun getCatalog(pollRoundId: Long): BookCatalogResponse {
        val pollRound = pollRoundRepository.findById(pollRoundId)
            .orElseThrow {
                IllegalArgumentException("Poll round not found: $pollRoundId")
            }

        val options = pollRoundOptionRepository
            .findAllByPollRoundIdOrderByOptionIndex(pollRoundId)

        val books = options.map { option ->
            CatalogBook(
                title = option.book.title,
                author = option.book.author,
                description = option.book.description,
                pageCount = option.book.pageCount,
                coverUrl = option.book.coverUrl
            )
        }

        return BookCatalogResponse(
            pollRoundId = pollRoundId,
            roundNumber = pollRound.roundNumber,
            books = books
        )
    }

    @Transactional(readOnly = true)
    fun getTelegramChatId(pollRoundId: Long): Long {
        val pollRound = pollRoundRepository.findById(pollRoundId)
            .orElseThrow {
                IllegalArgumentException("Poll round not found: $pollRoundId")
            }

        return pollRound.telegramChatId
    }
}