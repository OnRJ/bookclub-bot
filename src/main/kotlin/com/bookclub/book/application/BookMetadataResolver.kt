package com.bookclub.book.application

import org.springframework.stereotype.Component
import org.slf4j.LoggerFactory

@Component
class BookMetadataResolver(
    private val providers: List<BookMetadataProvider>,
    private val aggregator: BookMetadataAggregator
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun resolve(
        title: String,
        author: String? = null
    ): BookMetadataResolution? {

        if (title.isBlank()) return null

        val candidates = providers.flatMap { provider ->
            try {
                provider.search(title, author)
            } catch (exception: Exception) {
                log.warn(
                    "Metadata provider {} failed for '{}' (author='{}')",
                    provider::class.simpleName,
                    title,
                    author,
                    exception
                )
                emptyList()
            }
        }

        val ranked = candidates
            .distinctBy { candidate ->
                normalize(candidate.title) + "|" +
                        normalize(candidate.author.orEmpty()) + "|" +
                        candidate.source
            }
            .map { candidate ->
                BookMetadataResolution(
                    candidate = candidate,
                    score = calculateScore(title, author, candidate)
                )
            }
            .sortedByDescending { it.score }

        val eligible = ranked.filter { it.score >= MIN_SCORE }

        // Prefer a confident match, but keep the first provider result as a
        // fallback so that useful pages/ISBN/description data is not discarded.
        val best = eligible.firstOrNull()
            ?: candidates.firstOrNull()?.let {
                BookMetadataResolution(it, calculateScore(title, author, it))
            }
            ?: return null

        val merged = aggregator.merge(
            candidates = if (eligible.isNotEmpty()) {
                eligible.map { it.candidate }
            } else {
                candidates
            },
            preferred = best.candidate
        )

        return BookMetadataResolution(
            candidate = merged,
            score = best.score
        )
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
            } else {
                score -= AUTHOR_MISMATCH_PENALTY
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
        private const val AUTHOR_MISMATCH_PENALTY = 40
        private const val MIN_SCORE = 75
    }
}
