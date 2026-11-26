package com.bookclub.book.application

import org.springframework.stereotype.Component

@Component
class BookMetadataResolver(
    private val providers: List<BookMetadataProvider>
) {

    fun resolve(
        title: String,
        author: String? = null
    ): BookMetadataResolution? {

        if (title.isBlank()) return null

        val candidates = providers.flatMap { provider ->
            try {
                provider.search(title, author)
            } catch (_: Exception) {
                emptyList()
            }
        }

        val ranked = candidates
            .distinctBy { candidate ->
                normalize(candidate.title) + "|" + normalize(candidate.author.orEmpty())
            }
            .map { candidate ->
                BookMetadataResolution(
                    candidate = candidate,
                    score = calculateScore(title, author, candidate)
                )
            }
            .sortedByDescending { it.score }

        return ranked.firstOrNull { it.score >= MIN_SCORE }
    }

    private fun calculateScore(
        requestedTitle: String,
        requestedAuthor: String?,
        candidate: BookMetadataCandidate
    ): Int {

        val titleScore = calculateTitleScore(
            requestedTitle,
            candidate.title
        )

        if (titleScore == 0) return 0

        var score = titleScore

        if (!requestedAuthor.isNullOrBlank() &&
            !candidate.author.isNullOrBlank()
        ) {
            if (normalize(requestedAuthor) == normalize(candidate.author)) {
                score += AUTHOR_MATCH_BONUS
            }
        }

        return score
    }

    private fun calculateTitleScore(
        requestedTitle: String,
        candidateTitle: String
    ): Int {

        val requested = normalize(requestedTitle)
        val candidate = normalize(candidateTitle)

        if (requested.isBlank() || candidate.isBlank()) return 0

        return when {
            requested == candidate -> EXACT_TITLE_SCORE

            requested in candidate || candidate in requested ->
                PARTIAL_TITLE_SCORE

            else -> 0
        }
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()

    companion object {
        private const val EXACT_TITLE_SCORE = 80
        private const val PARTIAL_TITLE_SCORE = 40
        private const val AUTHOR_MATCH_BONUS = 20
        private const val MIN_SCORE = 75
    }
}