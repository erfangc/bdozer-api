package com.bdozer.api.web.controlleradvice

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant
import java.util.*

@ControllerAdvice
class AppControllerAdvice {

    private val log = LoggerFactory.getLogger(AppControllerAdvice::class.java)

    @ExceptionHandler(value = [IllegalStateException::class])
    fun handleException(ex: IllegalStateException): ResponseEntity<ApiError> {
        val id = UUID.randomUUID().toString()
        val error = ApiError(
            id = id,
            message = ex.message ?: "Unknown server error id=$id",
            timestamp = Instant.now()
        )
        val ret = ResponseEntity.badRequest().body(error)
        log.error(id, ex)
        return ret
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleInternalServerException(ex: Exception): ResponseEntity<ApiError> {
        val id = UUID.randomUUID().toString()
        val error = ApiError(
            id = id,
            message = ex.message ?: "Unknown server error id=$id",
            timestamp = Instant.now()
        )

        val ret = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error)

        log.error(id, ex)
        return ret
    }

}