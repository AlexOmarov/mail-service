package ru.somarov.mail.infrastructure.kafka.serde.dlq

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import ru.somarov.mail.presentation.kafka.DlqMessage
import ru.somarov.mail.presentation.kafka.event.CommonEvent

class DlqMessageSerializer(private val mapper: ObjectMapper) : Serializer<DlqMessage<out CommonEvent>> {
    @Suppress("TooGenericExceptionCaught")
    override fun serialize(topic: String, data: DlqMessage<out CommonEvent>): ByteArray {
        return try {
            mapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            throw SerializationException("Error when serializing DlqMessage to byte[]", e)
        }
    }
}
