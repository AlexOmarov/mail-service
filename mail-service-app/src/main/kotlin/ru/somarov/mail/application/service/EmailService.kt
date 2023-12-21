package ru.somarov.mail.application.service

import jakarta.mail.Message
import jakarta.mail.internet.InternetAddress
import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import java.time.OffsetDateTime

@Service
class EmailService(
    private val props: ServiceProps,
    private val mailRepo: MailRepo,
    private val emailSender: JavaMailSender
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    suspend fun sendLatestEmails(): Int {
        var iteration = 0
        var amountOfSentEmails = 0
        val startDate =
            OffsetDateTime.now().minusDays(props.contour.scheduling.emailSending.daysToCheckForUnsentEmails.toLong())
        log.info("Started to get mails with unsent emails starting from $startDate")
        var mailsWithMessages = mailRepo.findAllByIsEmailSentAndCreationDateAfter(
            false,
            startDate,
            Pageable.ofSize(props.contour.scheduling.emailSending.batchSize).withPage(0)
        ).toList().map { Pair(it, constructEmailMessage(it)) }
        while (mailsWithMessages.isNotEmpty()) {
            log.info("Mail sending, iteration $iteration")
            val sent = sendEmails(mailsWithMessages)
            processEmailsSending(mailsWithMessages.map { it.first }, sent)
            amountOfSentEmails += mailsWithMessages.size
            mailsWithMessages = mailRepo.findAllByIsEmailSentAndCreationDateAfter(
                false,
                startDate,
                Pageable.ofSize(props.contour.scheduling.emailSending.batchSize).withPage(0)
            ).toList().map { Pair(it, constructEmailMessage(it)) }
            iteration++
        }
        log.info("Finished sending emails for unprocessed mails from $startDate")
        return amountOfSentEmails
    }

    private suspend fun processEmailsSending(mails: List<Mail>, emailSent: Boolean): Boolean {
        if (emailSent) {
            log.info("Email for mails $mails has been sent. Updating mail data.")
            mails.forEach { mail ->
                mail.isEmailSent = true
                mail.lastUpdateDate = OffsetDateTime.now()
                mail.new = false
            }
            mailRepo.saveAll(mails).toList()
        }
        return true
    }

    // Have to do TooGenericExceptionCaught to catch all types of network exceptions
    // Have to do SpreadOperator because it is java mail sender specification
    @Suppress("TooGenericExceptionCaught", "SpreadOperator")
    private fun sendEmails(mailsWithMessages: List<Pair<Mail, MimeMessage>>): Boolean {
        var result = true
        try {
            mailsWithMessages.forEach { log.info("Sending message for mail ${it.first}") }
            emailSender.send(*(mailsWithMessages.map { it.second }.toTypedArray()))
        } catch (e: Exception) {
            log.error("Got exception while sending emails for mails ${mailsWithMessages.map { it.first }}: $e")
            result = false
        }
        return result
    }

    private fun constructEmailMessage(mail: Mail): MimeMessage {
        val channelCode =
            MailChannel.Companion.MailChannelCode.entries.first { it.id == mail.mailChannelId }.name
        val message = emailSender.createMimeMessage()
        message.setFrom(props.contour.mail.username)
        message.setRecipient(Message.RecipientType.TO, InternetAddress(props.contour.mail.destinationEmail))
        message.subject = "Обращение клиента ${mail.clientEmail}"
        message.setContent(
            """
                    <div style="vertical-align: middle">
                        <h1>Обращение от ${mail.creationDate}</h1>    
                        <p><b>Клиент: ${mail.clientId}</b></p>
                        <p><b>Email: ${mail.clientEmail ?: "Не указан"}</b></p>
                        <p><b>Телефон: ${mail.clientPhone}</b></p>
                        <p><b>ФИО: ${mail.clientFio}</b></p>
                        <p><b>Дата рождения: ${mail.clientBirthday}</b></p>
                        <p><b>Канал обращения: $channelCode</b></p>
                        <p>Текст: </p>
                        <p>${mail.text}</p>
                        
                        
                        <p><small>ID обращения: ${mail.id}</small></p>
                    </div>
            """.trimIndent(), "text/html; charset=UTF-8"
        )
        return message
    }
}
