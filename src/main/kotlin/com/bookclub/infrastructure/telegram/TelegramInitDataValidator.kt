package com.bookclub.infrastructure.telegram

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.time.Instant
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

@Component
class TelegramInitDataValidator(
    @param:Value("\${telegram.bot.token}")
    private val botToken: String,
    private val objectMapper: ObjectMapper
) {

    fun validate(initData: String): ValidatedTelegramData {
        require(initData.isNotBlank()) {
            "Telegram initData is missing"
        }

        val parameters = initData.split("&")
            .associate { parameter ->
                val parts = parameter.split("=", limit = 2)

                require(parts.size == 2) {
                    "Invalid initData parameter"
                }

                val key = URLDecoder.decode(
                    parts[0],
                    StandardCharsets.UTF_8
                )

                val value = URLDecoder.decode(
                    parts[1],
                    StandardCharsets.UTF_8
                )

                key to value
            }

        val receivedHash = parameters["hash"]
            ?: throw IllegalArgumentException("Missing Telegram hash")

        val dataCheckString = parameters
            .filterKeys { it != "hash" }
            .toSortedMap()
            .entries
            .joinToString("\n") { (key, value) ->
                "$key=$value"
            }

        val secretKey = hmacSha256(
            key = "WebAppData".toByteArray(StandardCharsets.UTF_8),
            data = botToken.toByteArray(StandardCharsets.UTF_8)
        )

        val calculatedHash = hmacSha256(
            key = secretKey,
            data = dataCheckString.toByteArray(StandardCharsets.UTF_8)
        ).joinToString("") { byte ->
            "%02x".format(byte)
        }

        require(
            MessageDigest.isEqual(
                calculatedHash.toByteArray(),
                receivedHash.toByteArray()
            )
        ) {
            "Invalid Telegram initData signature"
        }

        val authDate = parameters["auth_date"]
            ?.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid auth_date")

        val now = Instant.now()
        val authenticatedAt = Instant.ofEpochSecond(authDate)

        require(!authenticatedAt.isAfter(now.plusSeconds(60))) {
            "Telegram initData is from the future"
        }

        require(authenticatedAt.isAfter(now.minusSeconds(86400))) {
            "Telegram initData has expired"
        }

        val userJson = parameters["user"]
            ?: throw IllegalArgumentException("Missing Telegram user")

        val userNode = objectMapper.readTree(userJson)

        val telegramUserId = userNode["id"]?.asLong()
            ?: throw IllegalArgumentException("Missing Telegram user ID")

        val firstName = userNode["first_name"]?.asText()
            ?: throw IllegalArgumentException("Missing Telegram first name")

        val username = userNode["username"]?.asText()

        return ValidatedTelegramData(
            telegramUserId = telegramUserId,
            firstName = firstName,
            username = username,
            startParam = parameters["start_param"]
        )
    }

    private fun hmacSha256(
        key: ByteArray,
        data: ByteArray
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key, "HmacSHA256"))
        return mac.doFinal(data)
    }
}

data class ValidatedTelegramData(
    val telegramUserId: Long,
    val firstName: String,
    val username: String?,
    val startParam: String?
)
