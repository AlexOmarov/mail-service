package ru.somarov.mail.presentation.kafka.event.command

import ru.somarov.mail.presentation.kafka.event.AbstractCommonEvent
import ru.somarov.mail.presentation.kafka.event.EventType

data class CreateMailCommand(val email: String, val text: String) : AbstractCommonEvent() {
    override fun getType(): EventType {
        return EventType.CREATE_MAIL_COMMAND
    }
}
