package ru.somarov.mail.infrastructure.kafka.serde.retry

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import ru.somarov.mail.presentation.kafka.RetryMessage

class RetryMessageSerializer(private val mapper: ObjectMapper) : Serializer<RetryMessage<out Any>> {
    @Suppress("TooGenericExceptionCaught")
    override fun serialize(topic: String, data: RetryMessage<out Any>): ByteArray {
        return try {
            mapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            throw SerializationException("Error when serializing CommonEvent to byte[]", e)
        }
    }
}
