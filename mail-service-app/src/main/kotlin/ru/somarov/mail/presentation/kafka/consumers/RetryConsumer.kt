package ru.somarov.mail.presentation.kafka.consumers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import ru.somarov.mail.application.messagehandler.IMessageHandler
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.IMessageConsumer
import ru.somarov.mail.infrastructure.kafka.KafkaUtils.buildReceiver
import ru.somarov.mail.infrastructure.kafka.MessageMetadata
import ru.somarov.mail.infrastructure.kafka.serde.retry.RetryMessageDeserializer
import ru.somarov.mail.presentation.kafka.RetryMessage

@Component
class RetryConsumer(
    mapper: ObjectMapper,
    private val retryHandler: IMessageHandler<RetryMessage<Any>>,
    private val props: ServiceProps
) : IMessageConsumer<RetryMessage<Any>> {
    private val receiver = buildReceiver(RetryMessageDeserializer(mapper), props.kafka.retryTopic, props.kafka)

    override suspend fun handle(
        event: RetryMessage<Any>,
        metadata: MessageMetadata
    ) = retryHandler.handle(event, metadata)

    override fun getReceiver(): KafkaReceiver<String, RetryMessage<Any>?> = receiver

    override fun enabled(): Boolean = props.kafka.retryConsumingEnabled

    override fun getName(): String = "RETRY_RECEIVER"

    override fun getDelaySeconds() = props.kafka.retryHandlingInterval

    override fun getExecutionStrategy() = IMessageConsumer.ExecutionStrategy.SEQUENTIAL
}
