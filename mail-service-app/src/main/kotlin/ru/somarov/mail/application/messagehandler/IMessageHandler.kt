package ru.somarov.mail.application.messagehandler

import ru.somarov.mail.infrastructure.kafka.MessageConsumptionResult
import ru.somarov.mail.infrastructure.kafka.MessageMetadata

fun interface IMessageHandler<T> {
    suspend fun handle(message: T, metadata: MessageMetadata): MessageConsumptionResult
}
