package com.bookclub.shared.application

sealed class ApplicationException(message: String) : RuntimeException(message)

class ValidationException(message: String) : IllegalArgumentException(message)
class NotFoundException(message: String) : ApplicationException(message)
class ConflictException(message: String) : IllegalStateException(message)
class ExternalServiceException(message: String, cause: Throwable? = null) : ApplicationException(message) {
    init { initCause(cause) }
}
