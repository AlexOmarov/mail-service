package ru.somarov.mail.presentation.dto.event.broadcast

import java.util.UUID

data class MailBroadcast(
    val id: UUID,
    val status: MailStatus
) : java.io.Serializable {
    enum class MailStatus {
        NEW, SENT, FAILED
    }
}
