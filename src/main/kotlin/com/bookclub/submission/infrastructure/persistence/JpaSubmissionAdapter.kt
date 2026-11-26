
package com.bookclub.submission.infrastructure.persistence

import com.bookclub.book.infrastructure.persistence.BookEntity
import com.bookclub.book.infrastructure.persistence.BookMetadataStatus
import com.bookclub.book.infrastructure.persistence.BookRepository
import com.bookclub.club.infrastructure.persistence.ClubMemberEntity
import com.bookclub.club.infrastructure.persistence.ClubMemberRepository
import com.bookclub.club.infrastructure.persistence.ClubRepository
import com.bookclub.submission.application.*
import com.bookclub.submission.domain.BookListParser.ParsedBook
import com.bookclub.user.infrastructure.persistence.UserEntity
import com.bookclub.user.infrastructure.persistence.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth

@Component
class JpaSubmissionAdapter(
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val books: BookRepository,
    private val submissions: SubmissionRepository,
    private val submissionBooks: SubmissionBookRepository,
    private val clubMembers: ClubMemberRepository
) : SubmissionWriter {

    @Transactional
    override fun create(
        telegramChatId: Long,
        telegramUserId: Long,
        username: String?,
        firstName: String,
        readingMonth: YearMonth,
        books: List<ParsedBook>
    ): SubmissionResult {

        val club = clubs.findByTelegramChatId(telegramChatId)
            ?: error("Club not found")

        val user = users.findByTelegramUserId(telegramUserId)
            ?.also {
                it.username = username
                it.firstName = firstName
            }
            ?: users.save(
                UserEntity(
                    telegramUserId = telegramUserId,
                    username = username,
                    firstName = firstName
                )
            )

        val isMember = clubMembers
            .existsByClubTelegramChatIdAndUserTelegramUserId(
                telegramChatId,
                telegramUserId
            )

        if (!isMember) {
            clubMembers.save(
                ClubMemberEntity(
                    club = club,
                    user = user,
                    role = "MEMBER"
                )
            )
        }

        val monthDate = readingMonth.atDay(1)

        check(
            !submissions.existsByClubTelegramChatIdAndReadingMonth(
                telegramChatId,
                monthDate
            )
        ) {
            "A submission already exists for this club and month"
        }

        val submission = submissions.save(
            SubmissionEntity(
                club = club,
                submittedBy = user,
                readingMonth = monthDate
            )
        )

        val savedBooks = books.map { book ->
            findOrCreateBook(book)
        }

        submissionBooks.saveAll(
            savedBooks.mapIndexed { index, book ->
                SubmissionBookEntity(
                    submission = submission,
                    book = book,
                    position = index + 1
                )
            }
        )

        return SubmissionResult(
            requireNotNull(submission.id),
            readingMonth,
            telegramUserId,
            books.map { it.title }
        )
    }

    private fun findOrCreateBook(input: ParsedBook): BookEntity {
        val existing = books.findFirstByTitleIgnoreCase(input.title)

        if (existing != null) {
            return existing
        }

        return books.save(
            BookEntity(
                title = input.title,
                author = input.author,
                metadataStatus = BookMetadataStatus.NEW
            )
        )
    }
}
