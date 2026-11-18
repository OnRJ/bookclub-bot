package com.bookclub

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import org.springframework.scheduling.annotation.EnableScheduling

@EnableScheduling
@SpringBootApplication
class BookclubBotApplication

fun main(args: Array<String>) {
    runApplication<BookclubBotApplication>(*args)
}