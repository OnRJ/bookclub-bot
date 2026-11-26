package com.bookclub.poll.api

import com.bookclub.infrastructure.telegram.TelegramClubMembershipValidator
import com.bookclub.infrastructure.telegram.TelegramInitDataValidator
import com.bookclub.shared.application.ValidationException
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/book-catalog")
class BookCatalogController(
    private val validator: TelegramInitDataValidator,
    private val membershipValidator: TelegramClubMembershipValidator,
    private val bookCatalogService: BookCatalogService
) {

    @GetMapping("/{pollRoundId}")
    fun getCatalog(
        @PathVariable pollRoundId: Long,
        @RequestParam initData: String
    ): BookCatalogResponse {

        val telegramData = validator.validate(initData)

        val pollRound = bookCatalogService.getCatalog(pollRoundId)

        val telegramChatId = bookCatalogService.getTelegramChatId(pollRoundId)

        val isMember = membershipValidator.isMember(
            telegramChatId = telegramChatId,
            telegramUserId = telegramData.telegramUserId
        )

        if (!isMember) {
            throw ValidationException(
                "You must be a member of this club to view the book catalog"
            )
        }

        return pollRound
    }
}