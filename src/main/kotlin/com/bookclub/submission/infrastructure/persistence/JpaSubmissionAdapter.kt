
package com.bookclub.submission.infrastructure.persistence

import com.bookclub.book.application.BookDescriptionGenerator
import com.bookclub.book.application.BookDescriptionRequest
import com.bookclub.book.application.BookMetadataResolver
import com.bookclub.book.infrastructure.persistence.BookEntity
import com.bookclub.book.infrastructure.persistence.BookMetadataStatus
import com.bookclub.book.infrastructure.persistence.BookRepository
import com.bookclub.club.infrastructure.persistence.ClubMemberEntity
import com.bookclub.club.infrastructure.persistence.ClubMemberRepository
import com.bookclub.club.infrastructure.persistence.ClubRepository
import com.bookclub.submission.application.*
import com.bookclub.user.infrastructure.persistence.UserEntity
import com.bookclub.user.infrastructure.persistence.UserRepository
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.YearMonth
import java.time.Instant

@Component
class JpaSubmissionAdapter(
    private val clubs: ClubRepository,
    private val users: UserRepository,
    private val books: BookRepository,
    private val submissions: SubmissionRepository,
    private val submissionBooks: SubmissionBookRepository,
    private val clubMembers: ClubMemberRepository,
    private val metadataResolver: BookMetadataResolver,
    private val descriptionGenerator: BookDescriptionGenerator
) : SubmissionWriter {

    @Transactional
    override fun create(
        telegramChatId: Long,
        telegramUserId: Long,
        username: String?,
        firstName: String,
        readingMonth: YearMonth,
        bookTitles: List<String>
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

        val savedBooks = bookTitles.map { title ->
            enrichOrReuseBook(title)
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
            bookTitles
        )
    }

    private fun enrichOrReuseBook(title: String): BookEntity {
        val existing = books.findFirstByTitleIgnoreCase(title)

        if (existing != null &&
            existing.metadataStatus == BookMetadataStatus.READY &&
            !existing.description.isNullOrBlank()
        ) {
            return existing
        }

        val resolution = try {
            metadataResolver.resolve(title)
        } catch (_: Exception) {
            null
        }

        if (resolution == null) {
            return existing ?: books.save(
                BookEntity(
                    title = title,
                    metadataStatus = BookMetadataStatus.FAILED
                )
            )
        }

        val candidate = resolution.candidate

        val book = existing ?: BookEntity(title = title)

        if (book.author.isNullOrBlank() && !candidate.author.isNullOrBlank()) {
            book.author = candidate.author
        }

        if (book.pageCount == null && candidate.pageCount != null) {
            book.pageCount = candidate.pageCount
        }

        if (book.coverUrl.isNullOrBlank() && !candidate.coverUrl.isNullOrBlank()) {
            book.coverUrl = candidate.coverUrl
        }

        if (book.isbn.isNullOrBlank() && !candidate.isbn.isNullOrBlank()) {
            book.isbn = candidate.isbn
        }

        if (book.description.isNullOrBlank() &&
            !candidate.description.isNullOrBlank()
        ) {
            val generated = try {
                descriptionGenerator.generate(
                    BookDescriptionRequest(
                        title = candidate.title,
                        author = candidate.author,
                        sourceDescription = candidate.description,
                        pageCount = candidate.pageCount,
                        isbn = candidate.isbn,
                        metadataSource = candidate.source
                    )
                )
            } catch (_: Exception) {
                null
            }

            if (generated?.success == true &&
                !generated.description.isNullOrBlank()
            ) {
                book.description = generated.description
                book.descriptionSource = candidate.source
                book.metadataStatus = BookMetadataStatus.READY
            } else {
                book.descriptionSource = candidate.source
                book.metadataStatus = BookMetadataStatus.PARTIAL
            }
        } else if (book.description.isNullOrBlank()) {
            book.metadataStatus = BookMetadataStatus.PARTIAL
        }

        book.metadataUpdatedAt = Instant.now()

        return books.save(book)
    }
}
