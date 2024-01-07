package ru.somarov.mail.presentation.kafka

import ru.somarov.mail.presentation.kafka.event.EventType

data class DlqMessage<T>(
    val payload: T?,
    val key: String,
    val payloadType: EventType,
    var processingAttemptNumber: Int
)
