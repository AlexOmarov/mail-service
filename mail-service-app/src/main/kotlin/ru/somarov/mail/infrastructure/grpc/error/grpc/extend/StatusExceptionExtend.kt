package com.denumhub.error.grpc.extend

import com.denumhub.error.exception.technical.TechnicalException
import com.fasterxml.jackson.databind.ObjectMapper
import com.google.rpc.ErrorInfo
import io.grpc.StatusException
import io.grpc.protobuf.StatusProto
import org.slf4j.Logger

fun StatusException.unpackDetails(): ErrorInfo? {
    return StatusProto.fromThrowable(this)?.detailsList
        ?.filter { it.`is`(ErrorInfo::class.java) }
        ?.map { it.unpack(ErrorInfo::class.java) }
        ?.firstOrNull()
}

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
