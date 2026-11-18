package com.bookclub.submission.domain

import java.time.LocalDate
import java.time.YearMonth

class ReadingMonthPolicy(private val cutoffDay: Int = 10) {
    init {
        require(cutoffDay in 1..28)
    }

    fun determine(date: LocalDate): YearMonth {
        val month = YearMonth.from(date)
        return if (date.dayOfMonth <= cutoffDay) month else month.plusMonths(1)
    }
}
