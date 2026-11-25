
package com.bookclub.infrastructure.bookinfo

import com.fasterxml.jackson.databind.JsonNode
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class GoogleBooksService {

    private val restTemplate = RestTemplate()

    fun search(title: String, author: String? = null): BookInfo? {
        return try {
            val query = if (author.isNullOrBlank()) {
                title
            } else {
                "$title $author"
            }

            val uri = UriComponentsBuilder
                .fromUriString("https://www.googleapis.com/books/v1/volumes")
                .queryParam("q", query)
                .queryParam("maxResults", 5)
                .build()
                .encode()
                .toUri()

            val response = restTemplate.getForObject(uri, JsonNode::class.java)
                ?: return null

            val items = response.path("items")

            if (!items.isArray || items.isEmpty) {
                return null
            }

            val book = items[0].path("volumeInfo")

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
                title = book.path("title").asText(title),
                author = foundAuthor,
                description = description,
                pageCount = pageCount
            )
        } catch (_: Exception) {
            null
        }
    }
}
