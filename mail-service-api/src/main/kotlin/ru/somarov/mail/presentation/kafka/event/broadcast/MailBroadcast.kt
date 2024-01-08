package ru.somarov.mail.presentation.kafka.event.broadcast

import ru.somarov.mail.presentation.kafka.event.AbstractCommonEvent
import ru.somarov.mail.presentation.kafka.event.EventType
import ru.somarov.mail.presentation.kafka.event.broadcast.dto.MailStatus
import java.util.UUID

data class MailBroadcast(val id: UUID, val status: MailStatus) : AbstractCommonEvent {
    override fun getType(): EventType {
        return EventType.MAIL_BROADCAST
    }
}
