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
import ru.somarov.mail.infrastructure.db.entity.MailChannel.Companion.MailChannelCode
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import java.time.OffsetDateTime

@Service
class EmailService(
    private val props: ServiceProps,
    private val mailRepo: MailRepo,
    private val emailSender: JavaMailSender
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    suspend fun sendNewEmails(startDate: OffsetDateTime): Int {
        log.info("Started to get mails with unsent emails starting from $startDate")

        var iteration = 0
        var amountOfSentEmails = 0
        var mailsWithMessages: List<Pair<Mail, MimeMessage>>?

        do {
            log.info("Mail sending, iteration $iteration")

            mailsWithMessages = processIteration(startDate)
            amountOfSentEmails += mailsWithMessages.size

            iteration++

        } while (!mailsWithMessages.isNullOrEmpty())

        log.info("Finished sending emails for unprocessed mails from $startDate")
        return amountOfSentEmails
    }

    private suspend fun processIteration(startDate: OffsetDateTime): List<Pair<Mail, MimeMessage>> {
        val mailsWithMessages = mailRepo.findAllByIsEmailSentAndCreationDateAfter(
            false,
            startDate,
            Pageable.ofSize(props.contour.scheduling.emailSending.batchSize).withPage(0)
        ).toList().map { Pair(it, createMimeMessage(it)) }

        if (mailsWithMessages.isNotEmpty()) {
            val sent = sendMimeMessages(mailsWithMessages)
            if (sent) {
                saveSendingResult(mailsWithMessages.map { it.first })
            }
        }
        return mailsWithMessages
    }

    private suspend fun saveSendingResult(mails: List<Mail>) {
        log.info("Emails for mails $mails have been sent. Updating mail data.")
        mails.forEach { mail ->
            mail.mailStatusId = MailStatus.Companion.MailStatusCode.SENT.id
            mail.lastUpdateDate = OffsetDateTime.now()
            mail.new = false
        }
        mailRepo.saveAll(mails).toList()
    }

    // Have to do TooGenericExceptionCaught to catch all types of network exceptions
    // Have to do SpreadOperator because it is java mail sender specification
    @Suppress("TooGenericExceptionCaught", "SpreadOperator")
    private fun sendMimeMessages(mailsWithMessages: List<Pair<Mail, MimeMessage>>): Boolean {
        mailsWithMessages.forEach { log.info("Sending message for mail ${it.first}") }

        var result = true

        try {
            emailSender.send(*(mailsWithMessages.map { it.second }.toTypedArray()))
        } catch (e: Exception) {
            log.error("Got exception while sending emails for mails ${mailsWithMessages.map { it.first }}: $e")
            result = false
        }

        return result
    }

    private fun createMimeMessage(mail: Mail): MimeMessage {
        val message = emailSender.createMimeMessage()

        message.setFrom(props.contour.mail.username)
        message.setRecipient(Message.RecipientType.TO, InternetAddress(props.contour.mail.destinationEmail))
        message.subject = "Обращение клиента ${mail.clientEmail}"
        message.setContent(fillHtmlTemplate(mail), "text/html; charset=UTF-8")

        return message
    }

    private fun fillHtmlTemplate(mail: Mail): String {
        val channel = MailChannelCode.entries.first { it.id == mail.mailChannelId }.name
        return """
               <div style="vertical-align: middle">
                   <h1>Обращение от ${mail.creationDate}</h1>    
                   <p><b>Email: ${mail.clientEmail}</b></p>
                   <p><b>Канал обращения: $channel</b></p>
                   <p>Текст: </p>
                   <p>${mail.text}</p>
                   
                   <p><small>ID обращения: ${mail.id}</small></p>
               </div>
            """.trimIndent()
    }
}
