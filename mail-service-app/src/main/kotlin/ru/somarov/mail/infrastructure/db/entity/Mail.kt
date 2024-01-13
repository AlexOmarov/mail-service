package ru.somarov.mail.infrastructure.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table(value = "mail")
data class Mail(
    @Id
    @Column("id")
    val uuid: UUID,

    val clientEmail: String,
    val text: String,

    var mailStatusId: UUID,
    var mailChannelId: UUID,

    val creationDate: OffsetDateTime,
    var lastUpdateDate: OffsetDateTime,
) : Persistable<UUID> {
    @Transient
    var new: Boolean = true

    override fun getId(): UUID {
        return uuid
    }

    override fun isNew(): Boolean {
        return new
    }
}
