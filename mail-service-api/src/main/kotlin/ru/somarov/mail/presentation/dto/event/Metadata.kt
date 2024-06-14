package ru.somarov.mail.presentation.dto.event

import java.time.OffsetDateTime

data class Metadata(val createdAt: OffsetDateTime, val key: String, val attempt: Int)
