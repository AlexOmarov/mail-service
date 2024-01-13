package ru.somarov.mail.presentation.kafka.event.broadcast

import ru.somarov.mail.presentation.kafka.event.CommonEvent
import ru.somarov.mail.presentation.kafka.event.broadcast.dto.MailStatus
import java.util.UUID

data class MailBroadcast(val id: UUID, val status: MailStatus) : CommonEvent
