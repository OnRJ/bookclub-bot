
package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.TelegramBot
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TelegramBotConfig {

    @Bean
    fun telegramBot(
        @Value("\${telegram.bot.token}") token: String
    ): TelegramBot {
        return TelegramBot(token)
    }

    @Bean
    fun telegramBotCheck(
        telegramBotService: TelegramBotService
    ): CommandLineRunner {
        return CommandLineRunner {
            val response = telegramBotService.getBotInfo()

            if (response.isOk) {
                println(
                    "Telegram connected! Bot: ${response.user().firstName()}"
                )
            } else {
                println(
                    "Telegram error: ${response.description()}"
                )
            }
        }
    }

    @Bean
    fun telegramUpdates(
        telegramBot: TelegramBot,
        telegramUpdateHandler: TelegramUpdateHandler
    ): CommandLineRunner {
        return CommandLineRunner {
            telegramBot.setUpdatesListener(telegramUpdateHandler)
            println("Telegram update listener started!")
        }
    }
}
