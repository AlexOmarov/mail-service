package ru.somarov.mail.infrastructure.kafka.serde.createmailcommand

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.errors.SerializationException
import org.apache.kafka.common.serialization.Serializer
import ru.somarov.mail.presentation.kafka.event.command.CreateMailCommand

class CreateMailCommandSerializer(private val mapper: ObjectMapper) : Serializer<CreateMailCommand> {
    @Suppress("TooGenericExceptionCaught")
    override fun serialize(topic: String, data: CreateMailCommand): ByteArray {
        return try {
            mapper.writeValueAsBytes(data)
        } catch (e: Exception) {
            throw SerializationException("Error when serializing CreateMailCommand to byte[]", e)
        }
    }
}
