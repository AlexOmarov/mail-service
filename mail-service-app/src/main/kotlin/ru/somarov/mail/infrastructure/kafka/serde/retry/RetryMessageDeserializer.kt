package ru.somarov.mail.infrastructure.kafka.serde.retry

import com.fasterxml.jackson.databind.JavaType
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.kafka.common.serialization.Deserializer
import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.kafka.serde.createmailcommand.CreateMailCommandDeserializer
import ru.somarov.mail.presentation.kafka.RetryMessage
import ru.somarov.mail.presentation.kafka.event.EventType
import ru.somarov.mail.presentation.kafka.event.broadcast.MailBroadcast
import ru.somarov.mail.presentation.kafka.event.command.CreateMailCommand
import java.nio.charset.Charset

class RetryMessageDeserializer(private val mapper: ObjectMapper) : Deserializer<RetryMessage<Any>?> {
    private val log = LoggerFactory.getLogger(CreateMailCommandDeserializer::class.java)

    @Suppress("TooGenericExceptionCaught")
    override fun deserialize(topic: String, data: ByteArray): RetryMessage<Any>? {
        val rootNode = mapper.readValue(data, JsonNode::class.java)
        val payloadType = EventType.valueOf(rootNode.get(PAYLOAD_TYPE_NODE_NAME).textValue())
        val key = rootNode[KEY_NODE_NAME].textValue()
        val processingAttempts = rootNode.get(ATTEMPTS_TYPE_NODE_NAME).intValue()

        return try {
            val type: JavaType = when (payloadType) {
                EventType.MAIL_BROADCAST ->
                    mapper.typeFactory.constructParametricType(
                        RetryMessage::class.java,
                        MailBroadcast::class.java
                    )

                EventType.CREATE_MAIL_COMMAND ->
                    mapper.typeFactory.constructParametricType(
                        RetryMessage::class.java,
                        CreateMailCommand::class.java
                    )
            }
            mapper.readValue(String(data, Charset.forName("UTF-8")), type)
        } catch (e: Exception) {
            log.error("Got exception $e while trying to parse RetryEvent from data $data")
            return RetryMessage(
                payload = null,
                key = key,
                payloadType = payloadType,
                processingAttemptNumber = processingAttempts
            )
        }
    }

    companion object {
        private const val PAYLOAD_TYPE_NODE_NAME = "payloadType"
        private const val KEY_NODE_NAME = "key"
        private const val ATTEMPTS_TYPE_NODE_NAME = "processingAttempts"
    }
}
