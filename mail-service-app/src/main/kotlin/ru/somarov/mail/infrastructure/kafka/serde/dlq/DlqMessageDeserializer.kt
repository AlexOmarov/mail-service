package ru.somarov.mail.infrastructure.kafka.serde.dlq

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.header.Headers
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.kafka.Constants.PAYLOAD_TYPE_HEADER_NAME
import ru.somarov.mail.infrastructure.kafka.serde.retry.RetryMessageDeserializer
import ru.somarov.mail.presentation.dto.events.DlqMessage
import ru.somarov.mail.presentation.dto.events.event.CommonEvent
import java.nio.charset.Charset

class DlqMessageDeserializer(private val mapper: ObjectMapper) : Deserializer<DlqMessage<CommonEvent>?> {
    private val log = LoggerFactory.getLogger(RetryMessageDeserializer::class.java)
    override fun deserialize(p0: String, p1: ByteArray): DlqMessage<CommonEvent>? {
        throw IllegalStateException("Must not call deserialize method on DlqMessageDeserializer")
    }

    @Suppress("TooGenericExceptionCaught")
    override fun deserialize(topic: String, headers: Headers, data: ByteArray): DlqMessage<CommonEvent>? {
        val payloadType = String(headers.headers(PAYLOAD_TYPE_HEADER_NAME).first().value())
        val clazz = Class.forName(payloadType)

        return try {
            val type = mapper.typeFactory.constructParametricType(
                DlqMessage::class.java,
                clazz
            )
            mapper.readValue(String(data, Charset.forName("UTF-8")), type)
        } catch (e: Exception) {
            log.error("Got exception $e while trying to parse RetryEvent from data $data")
            return null
        }
    }
}
