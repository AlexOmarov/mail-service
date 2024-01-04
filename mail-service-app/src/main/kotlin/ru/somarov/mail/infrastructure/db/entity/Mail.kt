package ru.somarov.mail.infrastructure.db.entity

import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.springframework.data.annotation.Id
import org.springframework.data.annotation.Transient
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Table
import org.springframework.mail.javamail.JavaMailSender
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.entity.MailChannel.Companion.MailChannelCode
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

    fun createMimeMessage(mailProps: ServiceProps.MailProps, mailSender: JavaMailSender): MimeMessage {
        val message = mailSender.createMimeMessage()

        message.setFrom(mailProps.username)
        message.setRecipient(Message.RecipientType.TO, InternetAddress(mailProps.destinationEmail))
        message.subject = "Обращение клиента ${this.clientEmail}"
        message.setContent(fillHtmlTemplate(this), "text/html; charset=UTF-8")

        return message
    }

    private fun fillHtmlTemplate(mail: Mail): String {
        val channel = MailChannelCode.entries.first { it.id == mail.mailChannelId }.name
        return """
               <div style="vertical-align: middle">
                   <h1>Email от ${mail.creationDate}</h1>    
                   
                   <p><b>Email: ${mail.clientEmail}</b></p>
                   <p><b>Канал обращения: $channel</b></p>
                   <p>Текст: </p>
                   <p>${mail.text}</p>
                   
                   <p><small>ID обращения: ${mail.id}</small></p>
               </div>
        """.trimIndent()
    }
}
