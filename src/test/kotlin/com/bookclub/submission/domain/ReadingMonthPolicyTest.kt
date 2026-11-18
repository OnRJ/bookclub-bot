package com.bookclub.submission.domain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.YearMonth
class ReadingMonthPolicyTest {
 private val policy = ReadingMonthPolicy(10)
 @Test fun `cutoff day is current month`() {
  assertEquals(YearMonth.of(2026,11), policy.determine(LocalDate.of(2026,11,10)))
 }
 @Test fun `day after cutoff is next month`() {
  assertEquals(YearMonth.of(2026,12), policy.determine(LocalDate.of(2026,11,11)))
 }
 @Test fun `december rolls into next year`() {
  assertEquals(YearMonth.of(2027,1), policy.determine(LocalDate.of(2026,12,11)))
 }
}
