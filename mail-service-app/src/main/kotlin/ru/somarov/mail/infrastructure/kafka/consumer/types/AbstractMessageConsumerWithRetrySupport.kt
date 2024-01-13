package ru.somarov.mail.infrastructure.kafka.consumer.types

import org.slf4j.LoggerFactory
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.KafkaProducerFacade
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.presentation.kafka.event.CommonEvent
import kotlin.reflect.KClass

abstract class AbstractMessageConsumerWithRetrySupport<T : CommonEvent>(
    private val sender: KafkaProducerFacade,
    private val props: ServiceProps.KafkaProps
) : AbstractMessageConsumer<T>(props) {
    private val log = LoggerFactory.getLogger(this.javaClass)
    override suspend fun onFailedMessage(e: Exception?, message: T, metadata: MessageMetadata) {
        if (metadata.attempt < props.retryResendNumber) {
            sender.sendRetry(message, metadata.clone(metadata.attempt + 1))
        } else {
            sender.sendDlq(message, metadata)
        }
    }

    abstract suspend fun supports(): KClass<out T>
}
