package com.bookclub.infrastructure.bookinfo

import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class GoogleBooksService {

    private val logger = LoggerFactory.getLogger(GoogleBooksService::class.java)
    private val restTemplate = RestTemplate()

    fun search(title: String, author: String? = null): BookInfo? {
        return try {
            val query = if (author.isNullOrBlank()) {
                "intitle:$title"
            } else {
                "intitle:$title inauthor:$author"
            }

            val uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/books/v1/volumes")
                .queryParam("q", query)
                .queryParam("maxResults", 5)
                .queryParam("langRestrict", "ru")
                .build()
                .encode()
                .toUri()

            logger.info("Google Books request: {}", uri)

            val response = restTemplate.getForObject(uri, JsonNode::class.java)
                ?: return null

            val items = response.path("items")

            if (!items.isArray || items.isEmpty) {
                logger.info("Google Books: results not found for {}", title)
                return null
            }

            val book = items[0].path("volumeInfo")

            val foundTitle = book.path("title")
                .takeIf { !it.isMissingNode && !it.isNull }
                ?.asText()
                ?: title

            val authors = book.path("authors")
            val foundAuthor = if (authors.isArray && !authors.isEmpty) {
                authors.joinToString(", ") { it.asText() }
            } else {
                null
            }

            val description = book.path("description")
                .takeIf { !it.isMissingNode && !it.isNull }
                ?.asText()
                ?.takeIf { it.isNotBlank() }

            val pageCount = book.path("pageCount")
                .takeIf { it.isInt && it.asInt() > 0 }
                ?.asInt()

            BookInfo(
                title = foundTitle,
                author = foundAuthor,
                description = description,
                pageCount = pageCount
            )
        } catch (e: Exception) {
            logger.error("Google Books search failed for '{}'", title, e)
            null
        }
    }
}
