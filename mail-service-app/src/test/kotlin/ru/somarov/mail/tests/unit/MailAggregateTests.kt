package ru.somarov.mail.tests.unit

import jakarta.mail.Session
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.junit.jupiter.api.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mail.javamail.JavaMailSender
import ru.somarov.mail.application.aggregate.MailAggregate
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import java.time.OffsetDateTime
import java.util.UUID

private class MailAggregateTests {
    private val mailSender = mock<JavaMailSender>()

    @Test
    fun `When deserialize valid date then return OffsetDatetime object`() {
        doReturn(MimeMessage(null as Session?)).whenever(mailSender).createMimeMessage()
        val aggregate = MailAggregate(
            Mail(
                uuid = UUID.randomUUID(),
                clientEmail = "email",
                text = "text",
                mailStatusId = MailStatus.Companion.MailStatusCode.NEW.id,
                mailChannelId = MailChannel.Companion.MailChannelCode.MOBILE.id,
                creationDate = OffsetDateTime.now(),
                lastUpdateDate = OffsetDateTime.now()
            )
        )
        val message = aggregate.createMimeMessage(
            systemUser = "systemUser",
            destinationUser = "destinationUser",
            mailSender = mailSender,
            templatePath = "mail/templates/full_template.html"
        )
        assert(message.from.contentEquals(InternetAddress.parse("systemUser")))
        assert(
            message.getRecipients(MimeMessage.RecipientType.TO)
                .contentEquals(InternetAddress.parse("destinationUser"))
        )
        assert(message.subject == "Client ${aggregate.mail.clientEmail}")
        assert((message.content as String).contains(aggregate.mail.text))
    }
}
