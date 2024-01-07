package ru.somarov.mail.application.messagehandler

import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.kafka.KafkaSenderDecorator
import ru.somarov.mail.infrastructure.kafka.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.MessageMetadata
import ru.somarov.mail.presentation.kafka.event.EventType

abstract class AbstractRetryableMessageHandler<T : Any>(private val sender: KafkaSenderDecorator) : IMessageHandler<T> {
    private val logger = LoggerFactory.getLogger(AbstractRetryableMessageHandler::class.java)

    @Suppress("TooGenericExceptionCaught")
    override suspend fun handle(message: T, metadata: MessageMetadata): MessageConsumptionResult {
        logger.info("Got $message with metadata $metadata to handle with retry")
        return try {
            handleMessage(message, metadata)
        } catch (e: Exception) {
            logger.error("Got exception while processing event $message", e)
            sender.sendRetry(message, metadata, getPayloadType())
            MessageConsumptionResult(MessageConsumptionResult.MessageConsumptionResultCode.FAILED)
        }
    }

    abstract suspend fun handleMessage(message: T, metadata: MessageMetadata): MessageConsumptionResult

    abstract fun getPayloadType(): EventType
}
