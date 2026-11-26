package com.bookclub.book.application

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient

@Component
class OpenAiBookDescriptionGenerator(
    private val objectMapper: ObjectMapper,
    @Value("\${openai.api-key:}")
    private val apiKey: String,
    @Value("\${openai.model:gpt-4o-mini}")
    private val model: String
) : BookDescriptionGenerator {

    private val restClient = RestClient.create()

    override fun generate(
        request: BookDescriptionRequest
    ): BookDescriptionGenerationResult {

        if (apiKey.isBlank()) {
            return BookDescriptionGenerationResult(
                description = null,
                success = false,
                reason = "OpenAI API key is not configured"
            )
        }

        if (request.sourceDescription.isNullOrBlank()) {
            return BookDescriptionGenerationResult(
                description = null,
                success = false,
                reason = "No source facts available"
            )
        }

        return try {
            val payload = mapOf(
                "model" to model,
                "messages" to listOf(
                    mapOf(
                        "role" to "system",
                        "content" to """
                            Ты редактор книжного клуба.
                            Напиши оригинальное краткое описание
                            книги на русском языке.

                            Правила:
                            - 3-4 предложения.
                            - Без спойлеров.
                            - Используй только факты из предоставленного
                              источника.
                            - Не придумывай персонажей, события,
                              темы или обстоятельства.
                            - Не добавляй неподтвержденные факты.
                            - Если фактов недостаточно для описания,
                              верни пустое описание и sufficient=false.
                            - Не копируй исходное описание дословно.
                            - Не упоминай, что текст создан ИИ.
                        """.trimIndent()
                    ),
                    mapOf(
                        "role" to "user",
                        "content" to objectMapper.writeValueAsString(
                            mapOf(
                                "title" to request.title,
                                "author" to request.author,
                                "source_description" to request.sourceDescription,
                                "page_count" to request.pageCount,
                                "isbn" to request.isbn,
                                "source" to request.metadataSource
                            )
                        )
                    )
                ),
                "response_format" to mapOf(
                    "type" to "json_schema",
                    "json_schema" to mapOf(
                        "name" to "book_description",
                        "strict" to true,
                        "schema" to mapOf(
                            "type" to "object",
                            "properties" to mapOf(
                                "description" to mapOf(
                                    "type" to "string"
                                ),
                                "sufficient" to mapOf(
                                    "type" to "boolean"
                                ),
                                "reason" to mapOf(
                                    "type" to "string"
                                )
                            ),
                            "required" to listOf(
                                "description",
                                "sufficient",
                                "reason"
                            ),
                            "additionalProperties" to false
                        )
                    )
                )
            )

            val response = restClient.post()
                .uri("https://api.openai.com/v1/chat/completions")
                .header("Authorization", "Bearer $apiKey")
                .header("Content-Type", "application/json")
                .body(payload)
                .retrieve()
                .body(String::class.java)
                ?: throw IllegalStateException("Empty OpenAI response")

            val root = objectMapper.readTree(response)
            val content = root.path("choices")
                .path(0)
                .path("message")
                .path("content")
                .asText()

            val result = objectMapper.readTree(content)
            val sufficient = result.path("sufficient").asBoolean(false)
            val description = result.path("description")
                .asText()
                .trim()

            if (!sufficient || description.isBlank()) {
                BookDescriptionGenerationResult(
                    description = null,
                    success = false,
                    reason = result.path("reason").asText(
                        "Insufficient source facts"
                    )
                )
            } else {
                BookDescriptionGenerationResult(
                    description = description,
                    success = true,
                    reason = null
                )
            }
        } catch (e: Exception) {
            BookDescriptionGenerationResult(
                description = null,
                success = false,
                reason = "Description generation failed: ${e.message}"
            )
        }
    }
}
