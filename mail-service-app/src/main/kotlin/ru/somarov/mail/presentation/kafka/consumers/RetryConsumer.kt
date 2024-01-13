package ru.somarov.mail.presentation.kafka.consumers

import com.fasterxml.jackson.databind.ObjectMapper
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.KafkaProducerFacade
import ru.somarov.mail.infrastructure.kafka.consumer.IMessageConsumer
import ru.somarov.mail.infrastructure.kafka.consumer.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.infrastructure.kafka.consumer.types.AbstractMessageConsumer
import ru.somarov.mail.infrastructure.kafka.consumer.types.AbstractMessageConsumerWithRetrySupport
import ru.somarov.mail.infrastructure.kafka.serde.retry.RetryMessageDeserializer
import ru.somarov.mail.presentation.kafka.RetryMessage
import ru.somarov.mail.presentation.kafka.event.CommonEvent
import java.time.OffsetDateTime

@Component
class RetryConsumer(
    mapper: ObjectMapper,
    private val sender: KafkaProducerFacade,
    private val props: ServiceProps,
    private val consumers: List<AbstractMessageConsumerWithRetrySupport<CommonEvent>>,
) : AbstractMessageConsumer<RetryMessage<CommonEvent>>(props.kafka) {
    private val receiver = buildReceiver(RetryMessageDeserializer(mapper), props.kafka.retryTopic)
    private val log = LoggerFactory.getLogger(this.javaClass)
    override fun getReceiver(): KafkaReceiver<String, RetryMessage<CommonEvent>?> = receiver

    override fun enabled(): Boolean = props.kafka.retryConsumingEnabled

    override fun getName(): String = "RETRY_RECEIVER"

    override fun getDelaySeconds() = props.kafka.retryHandlingInterval

    override fun getExecutionStrategy() = IMessageConsumer.ExecutionStrategy.SEQUENTIAL
    override suspend fun onFailedMessage(e: Exception?, message: RetryMessage<CommonEvent>, metadata: MessageMetadata) {
        if (message.processingAttemptNumber < props.kafka.retryResendNumber) {
            sender.sendRetry(
                message.payload,
                MessageMetadata(OffsetDateTime.now(), message.key, message.processingAttemptNumber + 1)
            )
        } else {
            sender.sendDlq(
                message.payload,
                MessageMetadata(OffsetDateTime.now(), message.key, message.processingAttemptNumber + 1)
            )
        }
    }

    override suspend fun handleMessage(
        message: RetryMessage<CommonEvent>,
        metadata: MessageMetadata
    ): MessageConsumptionResult {
        return if (canBeRetried(message)) {
            retryMessage(message)
        } else {
            MessageConsumptionResult(MessageConsumptionResult.MessageConsumptionResultCode.FAILED)
        }
    }

    private suspend fun retryMessage(message: RetryMessage<CommonEvent>): MessageConsumptionResult {
        val consumer = consumers.firstOrNull { it.supports() == message.payload::class }
        if (consumer == null) {
            log.warn("Cannot find consumer for message $message. Skip message processing")
            return MessageConsumptionResult(MessageConsumptionResult.MessageConsumptionResultCode.OK)
        }
        return consumer.handleMessage(
            message.payload,
            MessageMetadata(OffsetDateTime.now(), message.key, message.processingAttemptNumber)
        )
    }

    // Need to not get all already produced retried messages with higher resend attempt number
    // when decreasing it in props
    private fun canBeRetried(message: RetryMessage<CommonEvent>): Boolean {
        return props.kafka.retryResendNumber > message.processingAttemptNumber
    }
}
