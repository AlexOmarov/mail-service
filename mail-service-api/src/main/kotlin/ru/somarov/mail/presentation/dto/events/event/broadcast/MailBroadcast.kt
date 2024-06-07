package ru.somarov.mail.presentation.dto.events.event.broadcast

import ru.somarov.mail.presentation.dto.events.event.CommonEvent
import ru.somarov.mail.presentation.dto.events.event.broadcast.dto.MailStatus
import java.util.UUID

data class MailBroadcast(val id: UUID, val status: MailStatus) : CommonEvent
