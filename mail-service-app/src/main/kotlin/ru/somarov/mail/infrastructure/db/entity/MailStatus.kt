package ru.somarov.mail.infrastructure.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Table
import java.util.UUID

@Table(value = "mail_status")
data class MailStatus(
    @Id
    val id: UUID,
    val code: String,
) {
    companion object {
        enum class MailStatusCode(val id: UUID) {
            NEW(UUID.fromString("0b35681b-fcc5-4140-adf1-4ff0da2acfac")),
            SENT(UUID.fromString("a453c3a3-394f-452c-bc07-b2dfdb6bc12e"))
        }
    }
}
