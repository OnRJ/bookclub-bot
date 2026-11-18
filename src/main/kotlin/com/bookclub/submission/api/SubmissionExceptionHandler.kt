
package com.bookclub.submission.api

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(
    val message: String
)

@RestControllerAdvice
class SubmissionExceptionHandler {

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleBadRequest(
        exception: IllegalArgumentException
    ): ResponseEntity<ApiError> =
        ResponseEntity
            .badRequest()
            .body(ApiError(exception.message ?: "Invalid request"))

    @ExceptionHandler(IllegalStateException::class)
    fun handleConflict(
        exception: IllegalStateException
    ): ResponseEntity<ApiError> =
        ResponseEntity
            .status(HttpStatus.CONFLICT)
            .body(ApiError(exception.message ?: "Submission conflict"))
}
