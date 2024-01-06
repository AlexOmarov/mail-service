package ru.somarov.mail.infrastructure.http

import ru.somarov.mail.infrastructure.grpc.error.exception.technical.TechnicalException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException
import ru.somarov.mail.presentation.rsocket.response.standard.ErrorResponse
import ru.somarov.mail.presentation.rsocket.response.standard.ResponseMetadata
import ru.somarov.mail.presentation.rsocket.response.standard.ResultCode
import ru.somarov.mail.presentation.rsocket.response.standard.StandardResponse

@RestControllerAdvice
class GlobalControllerExceptionHandler {

    private val log = LoggerFactory.getLogger(this.javaClass)

    @ExceptionHandler(Throwable::class)
    fun handleException(ex: Throwable): ResponseEntity<StandardResponse<ErrorResponse>> {
        log.error("Got exception while processing http request: $ex")
        val result = getResponseBody(ex)
        return ResponseEntity(
            StandardResponse(result.first, ResponseMetadata(ResultCode.FAILED, "Got error")),
            result.second
        )
    }

    private fun getResponseBody(exception: Throwable): Pair<ErrorResponse, HttpStatusCode> {
        return when (exception) {
            is ServerWebInputException -> processServerWebInputException(exception)
            is TechnicalException -> processTechnicalException(exception)
            else -> processDefaultException()
        }
    }

    private fun processTechnicalException(
        exception: TechnicalException
    ): Pair<ErrorResponse, HttpStatusCode> {
        val root = TechnicalException.getRootCause(exception)
        return ErrorResponse(mapOf("message" to root.message, "trace" to root.uniqueTrace)) to HttpStatusCode.valueOf(
            HTTP_ERROR_STATUS_CODE
        )
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
