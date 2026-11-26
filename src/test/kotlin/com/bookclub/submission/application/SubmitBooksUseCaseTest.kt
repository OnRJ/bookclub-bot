package com.bookclub.submission.application

import com.bookclub.submission.domain.ReadingMonthPolicy
import io.mockk.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.time.YearMonth

class SubmitBooksUseCaseTest {
    private val writer = mockk<SubmissionWriter>()
    private val clock = Clock.fixed(Instant.parse("2026-11-10T23:30:00Z"), ZoneOffset.UTC)
    private val useCase = SubmitBooksUseCase(writer, ReadingMonthPolicy(10), clock)
    private val books = (1..10).map { "Book $it" }

    @Test
    fun `uses UTC when determining reading month`() {
        every { writer.create(any(), any(), any(), any(), any(), any()) } returns
            SubmissionResult(1L, YearMonth.of(2026, 11), 7L, books)

        val result = useCase.execute(SubmitBooksCommand(-100L, 7L, "reader", "Reader", books))

        assertEquals(YearMonth.of(2026, 11), result.readingMonth)
        verify {
            writer.create(
                -100L, 7L, "reader", "Reader", YearMonth.of(2026, 11),
                books.map { com.bookclub.submission.domain.BookListParser.ParsedBook(it) }
            )
        }
    }

    @Test
    fun `rejects invalid input before accessing club`() {
        assertThrows(IllegalArgumentException::class.java) {
            useCase.execute(SubmitBooksCommand(100L, 7L, null, "Reader", books))
        }
        verify { writer wasNot Called }
    }

    @Test
    fun `rejects duplicate titles ignoring case`() {
        val duplicateBooks = books.dropLast(1) + " BOOK 1 "
        assertThrows(IllegalArgumentException::class.java) {
            useCase.execute(SubmitBooksCommand(-100L, 7L, null, "Reader", duplicateBooks))
        }
    }

}
