package ru.somarov.mail.infrastructure.kafka.serde.retry

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.kafka.Constants.PAYLOAD_TYPE_HEADER_NAME
import ru.somarov.mail.presentation.kafka.RetryMessage
import java.nio.charset.Charset

class RetryMessageDeserializer(private val mapper: ObjectMapper) : Deserializer<RetryMessage<Any>?> {
    private val log = LoggerFactory.getLogger(RetryMessageDeserializer::class.java)

    override fun deserialize(p0: String, p1: ByteArray): RetryMessage<Any>? {
        throw IllegalStateException("Must not call deserialize method on RetryMessageDeserializer")
    }

    @Suppress("TooGenericExceptionCaught")
    override fun deserialize(topic: String, headers: Headers, data: ByteArray): RetryMessage<Any>? {
        val payloadType = String(headers.headers(PAYLOAD_TYPE_HEADER_NAME).first().value())
        val clazz = Class.forName(payloadType)

        return try {
            val type = mapper.typeFactory.constructParametricType(
                RetryMessage::class.java,
                clazz
            )
            mapper.readValue(String(data, Charset.forName("UTF-8")), type)
        } catch (e: Exception) {
            log.error("Got exception $e while trying to parse RetryEvent from data $data")
            return null
        }
    }
}
