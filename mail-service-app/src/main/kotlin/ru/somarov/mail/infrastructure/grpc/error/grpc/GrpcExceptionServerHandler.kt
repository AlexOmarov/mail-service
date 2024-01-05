package ru.somarov.mail.infrastructure.grpc.error.grpc

import ru.somarov.mail.infrastructure.grpc.error.exception.technical.TechnicalException
import ru.somarov.mail.infrastructure.grpc.error.exception.technical.TechnicalExceptionRegistryMember
import ru.somarov.mail.infrastructure.grpc.error.exception.technical.TechnicalExceptionRegistryMember.BASE_TECHNICAL_EXCEPTION
import ru.somarov.mail.infrastructure.grpc.error.exception.technical.TechnicalExceptionRegistryMember.SIDE_SYSTEM_EXCEPTION
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.rpc.Code
import com.google.rpc.ErrorInfo
import com.google.rpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import org.slf4j.LoggerFactory
import org.springframework.boot.info.BuildProperties
import java.time.OffsetDateTime
import com.google.protobuf.Any as AnyProto

class GrpcExceptionServerHandler(private val mapper: ObjectMapper, private val buildProps: BuildProperties) {
    private val log = LoggerFactory.getLogger(this.javaClass)

    fun createStatusRuntimeException(exception: TechnicalException): StatusRuntimeException {
        log.error("Got technical exception while processing grpc request: $exception")
        return formStatusRuntimeException(
            TechnicalException(
                systemMessage = SIDE_SYSTEM_EXCEPTION.message,
                message = "Got technical exception from side system",
                cause = exception,
                code = SIDE_SYSTEM_EXCEPTION.code,
                datetime = OffsetDateTime.now(),
                serviceName = buildProps.name,
                uniqueTrace = exception.uniqueTrace
            )
        )
    }

    fun createStatusRuntimeException(): StatusRuntimeException {
        val exception = TechnicalException(
            systemMessage = BASE_TECHNICAL_EXCEPTION.message,
            message = "Got unknown exception",
            cause = null,
            code = BASE_TECHNICAL_EXCEPTION.code,
            datetime = OffsetDateTime.now(),
            serviceName = buildProps.name
        )
        log.error("Got exception while processing grpc request: $exception")
        return formStatusRuntimeException(exception)
    }

    fun createStatusRuntimeException(
        exception: Exception,
        registryMember: TechnicalExceptionRegistryMember
    ): StatusRuntimeException {
        log.error("Got common exception while processing grpc request: $exception")
        return formStatusRuntimeException(
            TechnicalException(
                systemMessage = registryMember.message,
                code = registryMember.code,
                datetime = OffsetDateTime.now(),
                serviceName = buildProps.name,
                cause = null,
                message = exception.message ?: "Got unknown exception"
            )
        )
    }

    private fun formStatusRuntimeException(exception: TechnicalException): StatusRuntimeException {

        val errorInfoBuilder = ErrorInfo.newBuilder()
            .putAllMetadata(
                mapOf(
                    "technicalException" to mapper.writeValueAsString(exception.also { it.stackTrace = arrayOf() })
                )
            )

        val status = Status.newBuilder()
            .setCode(Code.INTERNAL.number)
            .setMessage(exception.message)
            .addDetails(AnyProto.pack(errorInfoBuilder.build()))
            .build()
        return StatusProto.toStatusRuntimeException(status)
    }
}
