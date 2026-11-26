package com.bookclub.book.application

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import org.springframework.web.client.RestClient
import org.springframework.web.util.UriComponentsBuilder

@Component
class OpenLibraryMetadataProvider(
    private val objectMapper: ObjectMapper
) : BookMetadataProvider {

    private val restClient = RestClient.create()

    override fun search(
        title: String,
        author: String?
    ): List<BookMetadataCandidate> {

        if (title.isBlank()) return emptyList()

        val uriBuilder = UriComponentsBuilder
            .fromUriString("https://openlibrary.org/search.json")
            .queryParam("title", title.trim())
            .queryParam("limit", 10)
            .queryParam(
                "fields",
                "key,title,author_name,isbn,cover_i,number_of_pages_median"
            )

        if (!author.isNullOrBlank()) {
            uriBuilder.queryParam("author", author.trim())
        }

        val response = restClient.get()
            .uri(uriBuilder.build().encode().toUri())
            .retrieve()
            .body(String::class.java)
            ?: return emptyList()

        val root = objectMapper.readTree(response)
        val docs = root.path("docs")

        if (!docs.isArray) return emptyList()

        return docs.mapNotNull { doc ->
            parseCandidate(doc)
        }
    }

    private fun parseCandidate(doc: JsonNode): BookMetadataCandidate? {

        val title = doc.path("title")
            .takeIf { it.isTextual }
            ?.asText()
            ?.trim()
            ?: return null

        val authors = doc.path("author_name")
            .takeIf { it.isArray }
            ?.map { it.asText() }
            ?.filter { it.isNotBlank() }

        val author = authors?.joinToString(", ")

        val pageCount = doc.path("number_of_pages_median")
            .takeIf { it.isInt && it.asInt() > 0 }
            ?.asInt()

        val isbn = doc.path("isbn")
            .takeIf { it.isArray }
            ?.map { it.asText() }
            ?.firstOrNull { it.length == 13 }
            ?: doc.path("isbn")
                .takeIf { it.isArray }
                ?.map { it.asText() }
                ?.firstOrNull { it.length == 10 }

        val externalId = doc.path("key")
            .takeIf { it.isTextual }
            ?.asText()

        val description = externalId?.let { key ->
            fetchDescription(key)
        }

        return BookMetadataCandidate(
            title = title,
            author = author,
            description = description,
            pageCount = pageCount,
            isbn = isbn,
            externalId = externalId,
            source = "OPEN_LIBRARY"
        )
    }

    private fun fetchDescription(workKey: String): String? {

        if (!workKey.startsWith("/works/")) return null

        val uri = UriComponentsBuilder
            .fromUriString("https://openlibrary.org$workKey.json")
            .build()
            .toUri()

        return try {
            val response = restClient.get()
                .uri(uri)
                .retrieve()
                .body(String::class.java)
                ?: return null

            val root = objectMapper.readTree(response)
            val descriptionNode = root.path("description")

            when {
                descriptionNode.isTextual ->
                    descriptionNode.asText().trim().takeIf { it.isNotBlank() }

                descriptionNode.isObject ->
                    descriptionNode.path("value")
                        .takeIf { it.isTextual }
                        ?.asText()
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }

                else -> null
            }
        } catch (_: Exception) {
            null
        }
    }
}
