package ru.somarov.mail.infrastructure.kafka

import java.time.OffsetDateTime

data class MessageMetadata(val datetime: OffsetDateTime, val key: String, val attempt: Int)
