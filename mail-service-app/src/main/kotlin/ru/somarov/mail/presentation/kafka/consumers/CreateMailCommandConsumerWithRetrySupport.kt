package ru.somarov.mail.presentation.kafka.consumers

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.stereotype.Component
import reactor.kafka.receiver.KafkaReceiver
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.kafka.KafkaProducerFacade
import ru.somarov.mail.infrastructure.kafka.consumer.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.infrastructure.kafka.consumer.types.AbstractMessageConsumerWithRetrySupport
import ru.somarov.mail.infrastructure.kafka.serde.createmailcommand.CreateMailCommandDeserializer
import ru.somarov.mail.presentation.dto.events.event.command.CreateMailCommand

@Component
class CreateMailCommandConsumerWithRetrySupport(
    private val props: ServiceProps,
    private val service: MailService,
    mapper: ObjectMapper,
    sender: KafkaProducerFacade
) : AbstractMessageConsumerWithRetrySupport<CreateMailCommand>(
    sender = sender,
    props = props.kafka
) {
    private val postbackReceiver = buildReceiver(
        CreateMailCommandDeserializer(mapper),
        props.kafka.createMailCommandTopic
    )

    override suspend fun handleMessage(
        message: CreateMailCommand,
        metadata: MessageMetadata
    ): MessageConsumptionResult {
        service.createMail(message.email, message.text)
        return MessageConsumptionResult(MessageConsumptionResult.MessageConsumptionResultCode.OK)
    }

    override suspend fun supports() = CreateMailCommand::class

    override fun getReceiver(): KafkaReceiver<String, CreateMailCommand?> = postbackReceiver

    override fun enabled(): Boolean = props.kafka.createMailCommandConsumingEnabled

    override fun getName(): String = "CREATE_MAIL_COMMAND_RECEIVER"

    override fun getDelaySeconds() = null
}
