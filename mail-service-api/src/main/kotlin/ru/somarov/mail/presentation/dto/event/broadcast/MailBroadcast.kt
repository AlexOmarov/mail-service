package ru.somarov.mail.presentation.dto.event.broadcast

import kotlinx.serialization.Serializable
import ru.somarov.mail.serialization.UUIDSerializer
import java.util.UUID

@Serializable
data class MailBroadcast(
    @Serializable(with = UUIDSerializer::class)
    val id: UUID,
    val status: MailStatus
) : java.io.Serializable {
    enum class MailStatus {
        NEW, SENT, FAILED
    }
}
