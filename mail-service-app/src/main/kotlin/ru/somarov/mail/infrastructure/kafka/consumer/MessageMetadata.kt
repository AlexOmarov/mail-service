package ru.somarov.mail.infrastructure.kafka.consumer

import java.time.OffsetDateTime

data class MessageMetadata(val datetime: OffsetDateTime, val key: String, val attempt: Int) {
    fun clone(newAttempt: Int = attempt): MessageMetadata {
        return MessageMetadata(datetime, key, newAttempt)
    }
}
