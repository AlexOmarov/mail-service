package ru.somarov.mail.application.aggregate

import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import org.springframework.core.io.ClassPathResource
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.util.FileCopyUtils
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import kotlin.reflect.full.memberProperties

data class MailAggregate(val mail: Mail) {

    private val channel = MailChannel.Companion.MailChannelCode.entries.first { it.id == mail.mailChannelId }
    private val status = MailStatus.Companion.MailStatusCode.entries.first { it.id == mail.mailStatusId }

    fun createMimeMessage(
        systemUser: String,
        destinationUser: String,
        mailSender: JavaMailSender,
        templatePath: String
    ): MimeMessage {
        val message = mailSender.createMimeMessage()

        message.setFrom(systemUser)
        message.setRecipient(Message.RecipientType.TO, InternetAddress(destinationUser))
        message.subject = "Client ${mail.clientEmail}"
        message.setContent(fillHtmlTemplate(templatePath), "text/html; charset=UTF-8")

        return message
    }

    private fun fillHtmlTemplate(path: String): String {
        val resource = ClassPathResource(path)
        val inputStream = resource.inputStream
        var result = String(FileCopyUtils.copyToByteArray(inputStream), Charsets.UTF_8)
        val mailProperties = Mail::class.memberProperties

        for (property in mailProperties) {
            val placeholder = "\${${property.name}}"
            val propertyValue = property.get(mail)?.toString() ?: ""
            result = result.replace(placeholder, propertyValue)
        }

        result = result.replace("\${channel}", channel.name)
        result = result.replace("\${status}", status.name)

        return result
    }
}
