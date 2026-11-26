package com.bookclub.book.application

import org.springframework.stereotype.Component

@Component
class BookMetadataAggregator {

    fun merge(
        candidates: List<BookMetadataCandidate>,
        preferred: BookMetadataCandidate
    ): BookMetadataCandidate {

        val matchingCandidates = candidates.filter {
            normalize(it.title) == normalize(preferred.title)
        }

        val pool = (matchingCandidates + preferred).distinctBy {
            normalize(it.title) + "|" + normalize(it.author.orEmpty()) + "|" + it.source
        }

        fun firstNonBlank(selector: (BookMetadataCandidate) -> String?): String? =
            pool.firstNotNullOfOrNull { candidate ->
                selector(candidate)?.takeIf { it.isNotBlank() }
            }

        fun firstPositive(selector: (BookMetadataCandidate) -> Int?): Int? =
            pool.firstNotNullOfOrNull { candidate ->
                selector(candidate)?.takeIf { it > 0 }
            }

        val descriptions = pool.mapNotNull { candidate ->
            candidate.description?.takeIf { it.isNotBlank() }?.let {
                "${candidate.source}: $it"
            }
        }.distinct()

        val mergedDescription = descriptions.takeIf { it.isNotEmpty() }
            ?.joinToString("\n\n")

        return preferred.copy(
            author = preferred.author?.takeIf { it.isNotBlank() }
                ?: firstNonBlank { it.author },

            description = mergedDescription,

            pageCount = preferred.pageCount
                ?: firstPositive { it.pageCount },


            isbn = preferred.isbn?.takeIf { it.isNotBlank() }
                ?: firstNonBlank { it.isbn },

            source = pool.map { it.source }.distinct().joinToString("+")
        )
    }

    private fun normalize(value: String): String =
        value.lowercase()
            .replace(Regex("[^\\p{L}\\p{N}\\s]"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
}
