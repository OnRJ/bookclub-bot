package com.bookclub.submission.api

import com.bookclub.infrastructure.telegram.TelegramClubMembershipValidator
import com.bookclub.infrastructure.telegram.TelegramInitDataValidator
import com.bookclub.infrastructure.telegram.TelegramPollPublisher
import com.bookclub.poll.application.PollRoundService
import com.bookclub.poll.application.StartPollCommand
import com.bookclub.submission.application.SubmitBooksCommand
import com.bookclub.submission.application.SubmitBooksUseCase
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import com.bookclub.shared.application.ValidationException

@RestController
@RequestMapping("/api/submissions")
class SubmissionController(
    private val validator: TelegramInitDataValidator,
    private val submitBooksUseCase: SubmitBooksUseCase,
    private val membershipValidator: TelegramClubMembershipValidator,
    private val pollRoundService: PollRoundService,
    private val telegramPollPublisher: TelegramPollPublisher
) {

    @PostMapping
    fun submit(
        @RequestBody request: SubmitBooksRequest
    ): ResponseEntity<SubmitBooksResponse> {

        val telegramData = validator.validate(request.initData)

        val startParam = telegramData.startParam
            ?: throw ValidationException("Missing club parameter")

        val telegramChatId = startParam
            .removePrefix("club_")
            .toLongOrNull()
            ?: throw ValidationException("Invalid club parameter")

        if (!startParam.startsWith("club_")) throw ValidationException("Invalid club parameter")

        if (telegramChatId >= 0) throw ValidationException("Invalid Telegram group ID")

        val isMember = membershipValidator.isMember(
            telegramChatId = telegramChatId,
            telegramUserId = telegramData.telegramUserId
        )

        if (!isMember) throw ValidationException("You must be a member of this club to submit books")

        val result = submitBooksUseCase.execute(
            SubmitBooksCommand(
                telegramChatId = telegramChatId,
                telegramUserId = telegramData.telegramUserId,
                username = telegramData.username,
                firstName = telegramData.firstName,
                bookTitles = request.bookTitles
            )
        )

        val pollRound = pollRoundService.startFirstRound(
            StartPollCommand(
                submissionId = result.submissionId,
                telegramChatId = telegramChatId
            )
        )

        telegramPollPublisher.publishFirstRound(
            pollRound = pollRound,
            readingMonth = result.readingMonth
        )

        return ResponseEntity.status(HttpStatus.CREATED).body(
            SubmitBooksResponse(
                submissionId = result.submissionId,
                readingMonth = result.readingMonth,
                bookCount = result.bookTitles.size,
                message = "Список успешно отправлен!"
            )
        )
    }
}
