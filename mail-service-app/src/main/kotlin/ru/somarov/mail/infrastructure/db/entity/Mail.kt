package ru.somarov.mail.infrastructure.db.entity

import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import java.time.OffsetDateTime
import java.util.UUID

@Table(value = "mail")
data class Mail(
    @Id
    private var id: UUID,

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
        return id
    }

    fun setId(id: UUID) {
        this.id = id
    }

    override fun isNew(): Boolean {
        return new
    }
}
