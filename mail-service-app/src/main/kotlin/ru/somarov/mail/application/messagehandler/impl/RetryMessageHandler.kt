package ru.somarov.mail.application.messagehandler.impl

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.somarov.mail.application.messagehandler.AbstractRetryableMessageHandler
import ru.somarov.mail.application.messagehandler.IMessageHandler
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.MessageConsumptionResult.MessageConsumptionResultCode.FAILED
import ru.somarov.mail.infrastructure.kafka.MessageConsumptionResult.MessageConsumptionResultCode.OK
import ru.somarov.mail.infrastructure.kafka.MessageMetadata
import ru.somarov.mail.presentation.kafka.RetryMessage
import java.time.OffsetDateTime

@Service
class RetryMessageHandler(
    private val props: ServiceProps,
    private val handlers: List<AbstractRetryableMessageHandler<*>>
) : IMessageHandler<RetryMessage<Any>> {
    private val logger = LoggerFactory.getLogger(RetryMessageHandler::class.java)
    override suspend fun handle(message: RetryMessage<Any>, metadata: MessageMetadata): MessageConsumptionResult {
        return if (canBeRetried(message)) {
            retryMessage(message)
        } else {
            MessageConsumptionResult(FAILED)
        }
    }

    private suspend fun retryMessage(message: RetryMessage<Any>): MessageConsumptionResult {
        val handler = handlers.firstOrNull { it.getPayloadType() == message.payloadType }
        if (handler == null) {
            logger.warn("Cannot find retryable handler for message $message. Skip message processing")
            return MessageConsumptionResult(OK)
        }
        return callHandler(message, handler)
    }

    @Suppress("UNCHECKED_CAST")
    private suspend fun <T> callHandler(
        message: RetryMessage<Any>,
        handler: IMessageHandler<T>
    ): MessageConsumptionResult {
        return handler.handle(
            message.payload as T,
            MessageMetadata(OffsetDateTime.now(), message.key, message.processingAttemptNumber)
        )
    }

    private fun canBeRetried(message: RetryMessage<Any>): Boolean {
        return message.payload != null && props.kafka.retryResendNumber > message.processingAttemptNumber
    }
}
