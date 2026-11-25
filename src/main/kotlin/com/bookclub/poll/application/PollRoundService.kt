package com.bookclub.poll.application

import com.bookclub.poll.domain.PollRoundStatus

import com.bookclub.club.infrastructure.persistence.ClubMemberRepository
import com.bookclub.poll.infrastructure.persistence.*
import com.bookclub.submission.infrastructure.persistence.SubmissionBookRepository
import com.bookclub.submission.infrastructure.persistence.SubmissionRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Clock
import java.time.Duration
import java.time.Instant

@Service
class PollRoundService(
    private val submissions: SubmissionRepository,
    private val submissionBooks: SubmissionBookRepository,
    private val clubMembers: ClubMemberRepository,
    private val pollRounds: PollRoundRepository,
    private val pollRoundOptions: PollRoundOptionRepository,
    private val eligibleVoters: PollEligibleVoterRepository,
    private val clock: Clock = Clock.systemUTC()
) {

    @Transactional
    fun startFirstRound(command: StartPollCommand): PollRoundResult {
        // 1. Проверяем, что заявка существует и принадлежит этому клубу
        val submission = submissions.findByIdAndClubTelegramChatId(
            command.submissionId,
            command.telegramChatId
        ) ?: throw IllegalArgumentException("Submission not found")

        // 2. Не допускаем повторного создания первого раунда
        check(
            pollRounds.findBySubmissionIdAndRoundNumber(
                command.submissionId,
                1
            ) == null
        ) {
            "First poll round already exists"
        }

        // 3. Получаем книги заявки в исходном порядке
        val books = submissionBooks.findAllBySubmissionIdOrderByPosition(
            command.submissionId
        )

        check(books.size == 10) {
            "A poll must contain exactly 10 books"
        }

        // 4. Получаем участников клуба
        val members = clubMembers.findAllByClubTelegramChatId(
            command.telegramChatId
        )

        val voterIds = members
            .map { it.user.telegramUserId }
            .distinct()

        check(voterIds.isNotEmpty()) {
            "The club has no eligible voters"
        }

        // 5. Создаём раунд со сроком действия 3 дня
        val pollRound = pollRounds.save(
            PollRoundEntity(
                submission = submission,
                telegramChatId = command.telegramChatId,
                roundNumber = 1,
                status = PollRoundStatus.ACTIVE.name,
                closesAt = clock.instant().plus(Duration.ofDays(3))
            )
        )

        val pollRoundId = requireNotNull(pollRound.id)

        // 6. Сохраняем варианты голосования
        pollRoundOptions.saveAll(
            books.mapIndexed { index, submissionBook ->
                PollRoundOptionEntity(
                    pollRound = pollRound,
                    book = submissionBook.book,
                    optionIndex = index
                )
            }
        )

        // 7. Фиксируем список участников на момент запуска голосования
        eligibleVoters.saveAll(
            voterIds.map { telegramUserId ->
                PollEligibleVoterEntity(
                    pollRoundId = pollRoundId,
                    telegramUserId = telegramUserId
                )
            }
        )

        return PollRoundResult(
            pollRoundId = pollRoundId,
            submissionId = command.submissionId,
            roundNumber = 1,
            telegramChatId = command.telegramChatId,
            bookTitles = books.map { it.book.title },
            eligibleVotersCount = voterIds.size
        )
    }

    @Transactional
    fun saveTelegramPollInfo(
        pollRoundId: Long,
        telegramPollId: String,
        telegramMessageId: Long
    ) {
        val pollRound = pollRounds.findById(pollRoundId)
            .orElseThrow {
                IllegalArgumentException("Poll round not found: $pollRoundId")
            }

        pollRound.telegramPollId = telegramPollId
        pollRound.telegramMessageId = telegramMessageId

        pollRounds.save(pollRound)
    }

    @Transactional
    fun startRunoffRound(
        previousPollRoundId: Long,
        tiedBookIds: List<Long>
    ): PollRoundResult {
        require(tiedBookIds.size >= 2) {
            "A runoff poll requires at least two books"
        }

        val previousRound = pollRounds.findById(previousPollRoundId)
            .orElseThrow { IllegalArgumentException("Previous poll round not found") }

        check(PollRoundStatus.fromPersisted(previousRound.status) == PollRoundStatus.TIE) {
            "Previous poll round is not tied"
        }

        check(previousRound.roundNumber == 1) {
            "Runoff is allowed only after the first round"
        }

        val submissionId = requireNotNull(previousRound.submission.id)
        check(
            pollRounds.findBySubmissionIdAndRoundNumber(submissionId, 2) == null
        ) {
            "Second poll round already exists"
        }

        val previousOptions =
            pollRoundOptions.findAllByPollRoundIdOrderByOptionIndex(previousPollRoundId)

        val tiedOptions = previousOptions.filter {
            it.book.id in tiedBookIds
        }

        check(tiedOptions.size == tiedBookIds.distinct().size) {
            "Some tied books do not belong to the previous poll"
        }

        val newRound = pollRounds.save(
            PollRoundEntity(
                submission = previousRound.submission,
                telegramChatId = previousRound.telegramChatId,
                roundNumber = 2,
                status = PollRoundStatus.ACTIVE.name,
                closesAt = clock.instant().plus(Duration.ofDays(3))
            )
        )

        val newRoundId = requireNotNull(newRound.id)

        pollRoundOptions.saveAll(
            tiedOptions.mapIndexed { index, option ->
                PollRoundOptionEntity(
                    pollRound = newRound,
                    book = option.book,
                    optionIndex = index
                )
            }
        )

        val previousVoters = eligibleVoters.findAllByPollRoundId(previousPollRoundId)

        eligibleVoters.saveAll(
            previousVoters.map { voter ->
                PollEligibleVoterEntity(
                    pollRoundId = newRoundId,
                    telegramUserId = voter.telegramUserId
                )
            }
        )

        return PollRoundResult(
            pollRoundId = newRoundId,
            submissionId = submissionId,
            roundNumber = 2,
            telegramChatId = newRound.telegramChatId,
            bookTitles = tiedOptions.map { it.book.title },
            eligibleVotersCount = previousVoters.size
        )
    }

    @Transactional
    fun markRoundForAdminDecision(pollRoundId: Long) {
        val round = pollRounds.findById(pollRoundId).orElseThrow {
            IllegalArgumentException("Poll round not found")
        }

        check(PollRoundStatus.fromPersisted(round.status) == PollRoundStatus.ACTIVE) {
            "Poll round is not active"
        }

        round.status = PollRoundStatus.ADMIN_DECISION.name
    }
}
