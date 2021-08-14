package com.bdozer.api.web.controlleradvice

import com.bdozer.api.web.authn.RequestIdProvider
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant
import java.util.*

@ControllerAdvice
class AppControllerAdvice(private val requestIdProvider: RequestIdProvider) {

    private val log = LoggerFactory.getLogger(AppControllerAdvice::class.java)

    @ExceptionHandler(value = [IllegalStateException::class])
    fun handleException(ex: IllegalStateException): ResponseEntity<ApiError> {
        val requestId = requestIdProvider.get()
        val error = ApiError(
            id = requestId,
            message = ex.message ?: "Unknown server error id=$requestId",
            timestamp = Instant.now()
        )
        val ret = ResponseEntity.badRequest().body(error)
        log.error(requestId, ex)
        return ret
    }

    @ExceptionHandler(value = [Exception::class])
    fun handleInternalServerException(ex: Exception): ResponseEntity<ApiError> {
        val requestId = requestIdProvider.get()
        val error = ApiError(
            id = requestId,
            message = ex.message ?: "Unknown server error id=$requestId",
            timestamp = Instant.now()
        )

        val ret = ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(error)

        log.error(requestId, ex)
        return ret
    }

}