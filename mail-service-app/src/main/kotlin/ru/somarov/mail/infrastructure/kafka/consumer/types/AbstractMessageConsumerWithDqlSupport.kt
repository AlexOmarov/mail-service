package ru.somarov.mail.infrastructure.kafka.consumer.types

import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.KafkaProducerFacade
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.presentation.dto.events.event.CommonEvent

abstract class AbstractMessageConsumerWithDqlSupport<T : CommonEvent>(
    private val sender: KafkaProducerFacade,
    props: ServiceProps.KafkaProps
) : AbstractMessageConsumer<T>(props) {
    override suspend fun onFailedMessage(e: Exception?, message: T, metadata: MessageMetadata) {
        sender.sendDlq(message, metadata)
    }
}
