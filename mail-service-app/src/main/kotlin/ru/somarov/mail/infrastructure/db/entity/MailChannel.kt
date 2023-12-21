package ru.somarov.mail.infrastructure.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(value = "mail_channel")
data class MailChannel(
    @Id
    val id: UUID,
    val code: String,
) {
    companion object {
        enum class MailChannelCode(val id: UUID) {
            MOBILE(UUID.fromString("1e6a74e6-baa3-4185-8cbe-b5ce7061db52"))
        }
    }
}
