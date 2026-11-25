
package com.bookclub.submission.api

import com.bookclub.shared.application.*
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice

data class ApiError(
    val message: String
)

@RestControllerAdvice
class SubmissionExceptionHandler {

    @ExceptionHandler(ValidationException::class)
    fun handleValidation(exception: ValidationException): ResponseEntity<ApiError> =
        ResponseEntity.badRequest().body(ApiError(exception.message ?: "Invalid request"))

    @ExceptionHandler(NotFoundException::class)
    fun handleNotFound(exception: NotFoundException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiError(exception.message ?: "Not found"))

    @ExceptionHandler(ConflictException::class)
    fun handleConflictException(exception: ConflictException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.CONFLICT).body(ApiError(exception.message ?: "Conflict"))

    @ExceptionHandler(ExternalServiceException::class)
    fun handleExternalService(exception: ExternalServiceException): ResponseEntity<ApiError> =
        ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(ApiError(exception.message ?: "External service failure"))

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
