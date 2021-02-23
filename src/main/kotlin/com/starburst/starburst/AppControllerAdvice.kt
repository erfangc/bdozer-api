package com.starburst.starburst

import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import java.time.Instant
import java.util.*

data class Error(
    val id: String,
    val message: String,
    val timestamp: Instant
)

@ControllerAdvice
class AppControllerAdvice {

    private val log = LoggerFactory.getLogger(AppControllerAdvice::class.java)

    @ExceptionHandler(value = [Exception::class])
    fun genericError(ex: Exception): ResponseEntity<Error> {
        val id = UUID.randomUUID().toString()
        val error = Error(
            id = id ,
            message = ex.message ?: "Unknown server error id=$id",
            timestamp = Instant.now()
        )
        val ret = ResponseEntity.badRequest().body(error)
        log.error(id, ex)
        return ret
    }

}
