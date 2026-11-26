package com.bookclub.book.application

interface BookDescriptionGenerator {
    fun generate(request: BookDescriptionRequest): BookDescriptionGenerationResult
}
