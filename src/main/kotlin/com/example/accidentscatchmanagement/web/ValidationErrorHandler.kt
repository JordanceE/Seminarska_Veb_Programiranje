package com.example.accidentscatchmanagement.web

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ResponseStatusException

@RestControllerAdvice
class ValidationErrorHandler {
    @ExceptionHandler(IllegalArgumentException::class)
    fun invalid(exception: IllegalArgumentException) = ResponseEntity.badRequest()
        .body(mapOf("message" to (exception.message ?: "Invalid request")))

    @ExceptionHandler(ResponseStatusException::class)
    fun status(exception: ResponseStatusException) = ResponseEntity.status(exception.statusCode)
        .body(mapOf("message" to (exception.reason ?: "Request failed")))
}
