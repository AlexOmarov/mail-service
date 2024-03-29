package ru.somarov.mail.infrastructure.grpc.error.grpc.extend

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.rpc.ErrorInfo
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import org.slf4j.Logger
import ru.somarov.mail.infrastructure.grpc.error.exception.technical.TechnicalException

fun StatusException.unpackDetails(): ErrorInfo? {
    return StatusProto.fromThrowable(this)?.detailsList
        ?.filter { it.`is`(ErrorInfo::class.java) }
        ?.map { it.unpack(ErrorInfo::class.java) }
        ?.firstOrNull()
}

@Suppress("TooGenericExceptionCaught") // Should be able to swallow every exception
fun StatusException.toTechnicalException(mapper: ObjectMapper, logger: Logger): TechnicalException? {
    try {
        val details = this.unpackDetails()
        val ex = details?.metadataMap?.get("technicalException") ?: return null
        return mapper.readValue(ex, TechnicalException::class.java)
    } catch (e: Exception) {
        logger.error("Got exception while converting status exception to technical exception: $e")
        return null
    }
}
