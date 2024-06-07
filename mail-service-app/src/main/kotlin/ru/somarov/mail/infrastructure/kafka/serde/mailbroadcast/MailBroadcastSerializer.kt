package ru.somarov.mail.infrastructure.kafka.serde.mailbroadcast

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import ru.somarov.mail.presentation.dto.events.event.broadcast.MailBroadcast

class MailBroadcastSerializer(private val mapper: ObjectMapper) : Serializer<MailBroadcast> {
    @Suppress("TooGenericExceptionCaught")
    override fun serialize(topic: String, data: MailBroadcast): ByteArray {
        return try {
            mapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            throw SerializationException("Error when serializing MailBroadcast to byte[]", e)
        }
    }
}
