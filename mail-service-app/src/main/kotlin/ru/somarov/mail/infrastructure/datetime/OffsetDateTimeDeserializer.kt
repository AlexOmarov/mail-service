package ru.somarov.mail.infrastructure.datetime

import ru.somarov.mail.presentation.constants.MailServiceApiConstants.DATETIME_PATTERN
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

object OffsetDateTimeDeserializer {

    private val DATE_TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern(DATETIME_PATTERN)

    fun parse(text: String): OffsetDateTime {
        return OffsetDateTime.parse(text, DATE_TIME_FORMATTER)
    }
}
