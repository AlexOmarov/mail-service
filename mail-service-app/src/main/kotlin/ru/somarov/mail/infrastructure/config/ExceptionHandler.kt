package ru.somarov.mail.infrastructure.config

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.messaging.handler.annotation.MessageExceptionHandler
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.server.ServerWebInputException
import ru.somarov.mail.presentation.dto.response.ErrorResponse

@ControllerAdvice
private class ExceptionHandler {

    private val log = LoggerFactory.getLogger(this.javaClass)

    @ExceptionHandler(Throwable::class)
    fun handleException(ex: Throwable): ResponseEntity<ErrorResponse> {
        log.error("Got exception while processing http request: $ex")
        val result = getResponseBody(ex)
        return ResponseEntity(result.first, result.second)
    }

    @MessageExceptionHandler
    fun exception(ex: Exception): ResponseEntity<ErrorResponse> {
        log.error("Got exception while processing message: $ex")
        val result = getResponseBody(ex)
        return ResponseEntity(result.first, result.second)
    }

    private fun getResponseBody(exception: Throwable): Pair<ErrorResponse, HttpStatusCode> {
        return when (exception) {
            is ServerWebInputException -> processServerWebInputException(exception)
            else -> processDefaultException()
        }
    }

    private fun processServerWebInputException(
        exception: ServerWebInputException
    ): Pair<ErrorResponse, HttpStatusCode> {
        return ErrorResponse(mapOf("exception" to (exception.reason ?: ""))) to HttpStatusCode.valueOf(
            HTTP_ERROR_STATUS_CODE
        )
    }

    private fun processDefaultException(): Pair<ErrorResponse, HttpStatusCode> {
        return ErrorResponse(mapOf()) to HttpStatusCode.valueOf(HTTP_ERROR_STATUS_CODE)
    }

    companion object {
        private const val HTTP_ERROR_STATUS_CODE = 456
    }
}
