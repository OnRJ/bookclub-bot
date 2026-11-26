package com.bookclub.infrastructure.bookinfo

import com.bookclub.book.infrastructure.persistence.BookRepository
import com.fasterxml.jackson.databind.JsonNode
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate
import org.springframework.web.util.UriComponentsBuilder

@Service
class GoogleBooksService(
    private val bookRepository: BookRepository
) {

    private val logger = LoggerFactory.getLogger(GoogleBooksService::class.java)
    private val restTemplate = RestTemplate()
    private val apiKey = System.getenv("GOOGLE_BOOKS_API_KEY")

    fun search(title: String, author: String? = null): BookInfo? {
        return try {
            if (apiKey.isNullOrBlank()) {
                logger.warn("Google Books API key is not configured")
                return null
            }

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
                .queryParam("key", apiKey)
                .build()
                .encode()
                .toUri()

            logger.info("Google Books request for: {}", title)

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

            val coverUrl = book.path("imageLinks").path("thumbnail")
                .takeIf { !it.isMissingNode && !it.isNull }
                ?.asText()
                ?.replace("http://", "https://")

            val bookInfo = BookInfo(
                title = foundTitle,
                author = foundAuthor,
                description = description,
                pageCount = pageCount,
                coverUrl = coverUrl
            )

            saveBookInfo(title, bookInfo)

            bookInfo
        } catch (e: Exception) {
            logger.error("Google Books search failed for '{}'", title, e)
            null
        }
    }

    private fun saveBookInfo(originalTitle: String, info: BookInfo) {
        val book = bookRepository.findFirstByTitleIgnoreCase(originalTitle)

        if (book == null) {
            logger.warn("Book not found in database for saving: {}", originalTitle)
            return
        }

        info.author?.let { book.author = it }
        info.description?.let { book.description = it }
        info.pageCount?.let { book.pageCount = it }
        info.coverUrl?.let { book.coverUrl = it }

        book.externalSource = "Google Books"

        bookRepository.save(book)

        logger.info("Book info saved to database: {}", originalTitle)
    }
}