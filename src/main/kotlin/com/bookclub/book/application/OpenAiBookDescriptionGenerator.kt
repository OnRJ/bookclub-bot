package com.bookclub.book.application

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.http.client.JdkClientHttpRequestFactory
import java.net.http.HttpClient
import java.time.Duration
import org.slf4j.LoggerFactory

@Component
class OpenAiBookDescriptionGenerator(
    private val objectMapper: ObjectMapper,
    @param:Value($$"${openai.api-key}")
    private val apiKey: String,
    @param:Value($$"${openai.model:}")
    private val model: String
) : BookDescriptionGenerator {

    private val log = LoggerFactory.getLogger(javaClass)

    private val restClient = RestClient.builder()
        .requestFactory(
            JdkClientHttpRequestFactory(
                HttpClient.newBuilder()
                    .connectTimeout(Duration.ofSeconds(5))
                    .build()
            ).apply { setReadTimeout(Duration.ofSeconds(20)) }
        )
        .build()

    override fun generate(
        request: BookDescriptionRequest
    ): BookDescriptionGenerationResult {

        if (apiKey.isBlank()) {
            log.warn("AI description generation skipped: OPENAI_API_KEY is not configured")
            return BookDescriptionGenerationResult(
                description = null,
                success = false,
                reason = "OpenAI API key is not configured"
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
                            Определи метаданные книги и напиши оригинальное
                            краткое описание книги на русском языке.

                            Правила:
                            - 3-4 предложения.
                            - Без спойлеров.
                            - Если описание источника предоставлено,
                              используй только подтвержденные им факты.
                            - Если описания источника нет, используй только
                              общеизвестные сведения по названию и автору.
                            - Не придумывай персонажей, события,
                              темы или обстоятельства.
                            - Не добавляй неподтвержденные факты.
                            - Если надежных фактов недостаточно для описания,
                              верни пустое описание и sufficient=false.
                            - Не копируй исходное описание дословно.
                            - Не упоминай, что текст создан ИИ.
                            - author должен содержать автора, если он известен.
                            - page_count должен быть положительным числом, если
                              известен, иначе null.
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
                                ),
                                "author" to mapOf(
                                    "type" to listOf("string", "null")
                                ),
                                "page_count" to mapOf(
                                    "type" to listOf("integer", "null")
                                ),
                            ),
                            "required" to listOf(
                                "description",
                                "sufficient",
                                "reason",
                                "author",
                                "page_count"
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
            val author = result.path("author")
                .takeIf { it.isTextual }
                ?.asText()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
            val pageCount = result.path("page_count")
                .takeIf { it.isInt && it.asInt() > 0 }
                ?.asInt()

            if (!sufficient || description.isBlank()) {
                BookDescriptionGenerationResult(
                    description = null,
                    success = false,
                    reason = result.path("reason").asText(
                        "Insufficient source facts"
                    ),
                    author = author,
                    pageCount = pageCount
                )
            } else {
                BookDescriptionGenerationResult(
                    description = description,
                    success = true,
                    reason = null,
                    author = author,
                    pageCount = pageCount
                )
            }
        } catch (e: Exception) {
            log.error("AI description generation failed for '{}'", request.title, e)
            BookDescriptionGenerationResult(
                description = null,
                success = false,
                reason = "Description generation failed: ${e.message}"
            )
        }
    }
}
