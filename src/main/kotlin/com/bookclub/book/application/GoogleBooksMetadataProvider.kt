package com.bookclub.book.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class GoogleBooksMetadataProvider(
    private val objectMapper: ObjectMapper,
    @Value("\${google.books.api-key:}")
    private val apiKey: String
) : BookMetadataProvider {

    private val restClient = RestClient.create()

    override fun search(
        title: String,
        author: String?
    ): List<BookMetadataCandidate> {

        if (title.isBlank()) return emptyList()

        val query = buildString {
            append("intitle:")
            append(title.trim())

            if (!author.isNullOrBlank()) {
                append(" inauthor:")
                append(author.trim())
            }
        }

        val uriBuilder = UriComponentsBuilder
            .fromUriString("https://www.googleapis.com/books/v1/volumes")
            .queryParam("q", query)
            .queryParam("maxResults", 10)

        if (apiKey.isNotBlank()) {
            uriBuilder.queryParam("key", apiKey)
        }

        val response = restClient.get()
            .uri(uriBuilder.build().encode().toUri())
            .retrieve()
            .body(String::class.java)
            ?: return emptyList()

        val root = objectMapper.readTree(response)
        val items = root.path("items")

        if (!items.isArray) return emptyList()

        return items.mapNotNull { item ->
            parseCandidate(item)
        }
    }

    private fun parseCandidate(item: JsonNode): BookMetadataCandidate? {

        val volumeInfo = item.path("volumeInfo")

        val title = volumeInfo.path("title")
            .takeIf { it.isTextual }
            ?.asText()
            ?.trim()
            ?: return null

        val authors = volumeInfo.path("authors")
            .takeIf { it.isArray }
            ?.map { it.asText() }
            ?.filter { it.isNotBlank() }

        val author = authors?.joinToString(", ")

        val description = volumeInfo.path("description")
            .takeIf { it.isTextual }
            ?.asText()
            ?.trim()
            ?.takeIf { it.isNotBlank() }

        val pageCount = volumeInfo.path("pageCount")
            .takeIf { it.isInt && it.asInt() > 0 }
            ?.asInt()

        val coverUrl = volumeInfo
            .path("imageLinks")
            .path("thumbnail")
            .takeIf { it.isTextual }
            ?.asText()
            ?.replace("http://", "https://")

        val isbn = volumeInfo.path("industryIdentifiers")
            .takeIf { it.isArray }
            ?.firstOrNull {
                it.path("type").asText() == "ISBN_13"
            }
            ?.path("identifier")
            ?.asText()
            ?: volumeInfo.path("industryIdentifiers")
                .takeIf { it.isArray }
                ?.firstOrNull {
                    it.path("type").asText() == "ISBN_10"
                }
                ?.path("identifier")
                ?.asText()

        return BookMetadataCandidate(
            title = title,
            author = author,
            description = description,
            pageCount = pageCount,
            coverUrl = coverUrl,
            isbn = isbn,
            externalId = item.path("id")
                .takeIf { it.isTextual }
                ?.asText(),
            source = "GOOGLE_BOOKS"
        )
    }
}