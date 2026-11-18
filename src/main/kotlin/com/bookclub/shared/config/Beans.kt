package com.bookclub.shared.config

import com.bookclub.submission.domain.ReadingMonthPolicy
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.time.Clock

@Configuration
class Beans {
    @Bean
    fun clock(): Clock = Clock.systemUTC()

    @Bean
    fun readingMonthPolicy(@Value($$"${app.submission.current-month-cutoff-day:10}") cutoff: Int) =
        ReadingMonthPolicy(cutoff)
}
