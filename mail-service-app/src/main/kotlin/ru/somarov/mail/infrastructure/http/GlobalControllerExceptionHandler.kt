package ru.somarov.mail.infrastructure.http

import com.denumhub.error.exception.technical.TechnicalException
import com.denumhub.error.grpc.extend.toTechnicalException
import com.denumhub.http.response.ApiErrorResponse
import com.fasterxml.jackson.databind.ObjectMapper
import io.grpc.StatusException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.ServerWebInputException

@RestControllerAdvice
class GlobalControllerExceptionHandler(
    val objectMapper: ObjectMapper
) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    @ExceptionHandler(Throwable::class)
    fun handleException(ex: Throwable): ResponseEntity<ApiErrorResponse> {
        log.error("Got exception while processing http request: $ex")
        val result = getResponseBody(ex)
        return ResponseEntity(result.first, result.second)
    }

    private fun getResponseBody(
        exception: Throwable
    ): Pair<ApiErrorResponse, HttpStatusCode> {
        return when (exception) {
            is ServerWebInputException -> processServerWebInputException(exception)
            is StatusException -> processStatusException(exception)
            is TechnicalException -> processTechnicalException(exception)
            else -> processDefaultException()
        }
    }

    private fun processTechnicalException(
        exception: TechnicalException
    ): Pair<ApiErrorResponse, HttpStatusCode> {
        val root = TechnicalException.getRootCause(exception)
        return ApiErrorResponse(
            code = root.code,
            message = root.message,
            errorTraceCode = root.uniqueTrace,
            data = null
        ) to HttpStatusCode.valueOf(HTTP_ERROR_STATUS_CODE)
    }

    private fun processServerWebInputException(
        exception: ServerWebInputException
    ): Pair<ApiErrorResponse, HttpStatusCode> {
        return ApiErrorResponse(
            code = ApiErrorCode.SERVER_WEB_INPUT.code,
            message = exception.reason ?: ""
        ) to HttpStatusCode.valueOf(HTTP_ERROR_STATUS_CODE)
    }

    private fun processStatusException(
        exception: StatusException
    ): Pair<ApiErrorResponse, HttpStatusCode> {
        val baseException = exception.toTechnicalException(objectMapper, log)
        return if (baseException != null) {
            processTechnicalException(baseException)
        } else {
            ApiErrorResponse(
                code = ApiErrorCode.STATUS.code,
                message = "Side system error: (status: ${exception.status}, message: ${exception.message})"
            ) to HttpStatusCode.valueOf(HTTP_ERROR_STATUS_CODE)
        }
    }

    private fun processDefaultException(): Pair<ApiErrorResponse, HttpStatusCode> {
        return ApiErrorResponse(
            code = ApiErrorCode.DEFAULT.code,
            message = "Internal method error."
        ) to HttpStatusCode.valueOf(HTTP_ERROR_STATUS_CODE)
    }

    companion object {
        private const val HTTP_ERROR_STATUS_CODE = 456

        enum class ApiErrorCode(val code: Int) {
            DEFAULT(code = 1000),
            STATUS(code = 1999),
            SERVER_WEB_INPUT(code = 1100)
        }
    }
}
