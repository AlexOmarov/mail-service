package ru.somarov.mail.infrastructure.grpc.error.exception.technical

import java.time.OffsetDateTime
import java.util.UUID

data class TechnicalException(
    override val message: String,
    override val cause: TechnicalException?,
    val code: Int,
    val systemMessage: String,
    val serviceName: String,
    val datetime: OffsetDateTime,
    val uniqueTrace: String = generateTrace()
) : Exception(message, cause) {

    companion object {
        private const val MAX_EXCEPTION_NESTING = 100
        private const val TRACE_UPPER_BOUND = 6
        private const val TRACE_LOWER_BOUND = 0

        fun getRootCause(ex: TechnicalException): TechnicalException {
            var rootCause = ex
            var nestingIndex = 0
            while (rootCause.cause != null && nestingIndex < MAX_EXCEPTION_NESTING) {
                rootCause = rootCause.cause!!
                nestingIndex += 1
            }
            return rootCause
        }

        fun generateTrace(): String {
            return UUID.randomUUID().toString().substring(TRACE_LOWER_BOUND..TRACE_UPPER_BOUND)
        }
    }
}
