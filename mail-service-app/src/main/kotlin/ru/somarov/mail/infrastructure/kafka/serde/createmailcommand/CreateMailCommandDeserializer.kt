package ru.somarov.mail.infrastructure.kafka.serde.createmailcommand

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.kafka.serde.retry.RetryMessageDeserializer
import ru.somarov.mail.presentation.dto.events.event.command.CreateMailCommand
import java.nio.charset.Charset

class CreateMailCommandDeserializer(private val mapper: ObjectMapper) : Deserializer<CreateMailCommand?> {
    private val log = LoggerFactory.getLogger(RetryMessageDeserializer::class.java)

    @Suppress("TooGenericExceptionCaught")
    override fun deserialize(topic: String, data: ByteArray): CreateMailCommand? {
        val stringData = String(data, Charset.forName("UTF-8"))
        return try {
            mapper.readValue(stringData, CreateMailCommand::class.java)
        } catch (e: Exception) {
            log.error("Got exception $e while trying to parse ConversionUpdateEvent from data $data")
            return null
        }
    }
}
