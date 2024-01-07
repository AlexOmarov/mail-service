package ru.somarov.mail.application.messagehandler.impl

import org.springframework.stereotype.Service
import ru.somarov.mail.application.messagehandler.AbstractRetryableMessageHandler
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.infrastructure.kafka.KafkaSenderDecorator
import ru.somarov.mail.infrastructure.kafka.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.MessageConsumptionResult.MessageConsumptionResultCode.OK
import ru.somarov.mail.infrastructure.kafka.MessageMetadata
import ru.somarov.mail.presentation.kafka.event.EventType
import ru.somarov.mail.presentation.kafka.event.command.CreateMailCommand

@Service
class CreateMailCommandMessageHandler(
    private val service: MailService,
    sender: KafkaSenderDecorator
) : AbstractRetryableMessageHandler<CreateMailCommand>(sender) {

    override suspend fun handleMessage(
        message: CreateMailCommand,
        metadata: MessageMetadata
    ): MessageConsumptionResult {
        service.createMail(message.email, message.text)
        return MessageConsumptionResult(OK)
    }

    override fun getPayloadType() = EventType.CREATE_MAIL_COMMAND
}
