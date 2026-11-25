
package com.bookclub.infrastructure.telegram

import com.pengrad.telegrambot.TelegramBot
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Bean
import org.slf4j.LoggerFactory
import org.springframework.context.annotation.Configuration
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

@Configuration
class TelegramBotConfig {
    private val log = LoggerFactory.getLogger(javaClass)

    @Bean
    fun telegramBot(
        @Value("\${telegram.bot.token:}") token: String
    ): TelegramBot {
        return TelegramBot(token)
    }

    @Bean
    @ConditionalOnProperty(prefix = "telegram", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun telegramBotCheck(
        telegramBotService: TelegramBotService
    ): CommandLineRunner {
        return CommandLineRunner {
            val response = telegramBotService.getBotInfo()

            if (response.isOk) {
                log.info("Telegram connected. Bot: {}", response.user().firstName())
            } else {
                log.error("Telegram connection failed: {}", response.description())
            }
        }
    }

    @Bean
    @ConditionalOnProperty(prefix = "telegram", name = ["enabled"], havingValue = "true", matchIfMissing = true)
    fun telegramUpdates(
        telegramBot: TelegramBot,
        telegramUpdateHandler: TelegramUpdateHandler
    ): CommandLineRunner {
        return CommandLineRunner {
            telegramBot.setUpdatesListener(telegramUpdateHandler)
            log.info("Telegram update listener started")
        }
    }
}
