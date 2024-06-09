package ru.somarov.mail.infrastructure.kafka

import java.time.OffsetDateTime

data class Metadata(val createdAt: OffsetDateTime, val key: String, val attempt: Int)
