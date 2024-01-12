package ru.somarov.mail.presentation.kafka.consumers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import ru.somarov.mail.application.messagehandler.impl.CreateMailCommandMessageHandler
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.IMessageConsumer
import ru.somarov.mail.infrastructure.kafka.KafkaUtils.buildReceiver
import ru.somarov.mail.infrastructure.kafka.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.MessageMetadata
import ru.somarov.mail.infrastructure.kafka.serde.createmailcommand.CreateMailCommandDeserializer
import ru.somarov.mail.presentation.kafka.event.command.CreateMailCommand

// TODO: redo kafka processing
@Component
class CreateMailCommandConsumer(
    mapper: ObjectMapper,
    private val postbackNotificationHandler: CreateMailCommandMessageHandler,
    private val props: ServiceProps
) : IMessageConsumer<CreateMailCommand> {
    private val postbackReceiver = buildReceiver(
        CreateMailCommandDeserializer(mapper),
        props.kafka.createMailCommandTopic,
        props.kafka
    )

    override suspend fun handle(event: CreateMailCommand, metadata: MessageMetadata): MessageConsumptionResult =
        postbackNotificationHandler.handle(event, metadata)

    override fun getReceiver(): KafkaReceiver<String, CreateMailCommand?> = postbackReceiver

    override fun enabled(): Boolean = props.kafka.createMailCommandConsumingEnabled

    override fun getName(): String = "CREATE_MAIL_COMMAND_RECEIVER"

    override fun getDelaySeconds() = null
}
