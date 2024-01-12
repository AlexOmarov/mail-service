package ru.somarov.mail.application.service

import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.FAILED
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.NEW
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.SENT
import ru.somarov.mail.infrastructure.kafka.KafkaSenderDecorator
import ru.somarov.mail.infrastructure.kafka.MessageMetadata
import ru.somarov.mail.infrastructure.mail.EmailSenderFacade
import ru.somarov.mail.presentation.kafka.event.broadcast.MailBroadcast
import ru.somarov.mail.presentation.kafka.event.broadcast.dto.MailStatus
import java.time.OffsetDateTime

@Service
class EmailService(
    private val props: ServiceProps,
    private val dao: Dao,
    private val kafkaSenderDecorator: KafkaSenderDecorator,
    private val emailSenderFacade: EmailSenderFacade
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    suspend fun sendNewEmails(startDate: OffsetDateTime): Int {
        log.info("Started to get mails with unsent emails starting from $startDate")

        var iteration = 0
        var amountOfSentEmails = 0
        var mails: List<Mail>?
        val batchSize = props.contour.scheduling.emailSending.batchSize

        do {
            log.info("Mail sending, iteration $iteration")

            mails = getMailsByCreationDate(startDate, batchSize)
            sendMails(mails)
            amountOfSentEmails += mails.size

            iteration++
        } while (!mails.isNullOrEmpty())

        log.info("Finished sending emails for unprocessed mails from $startDate")
        return amountOfSentEmails
    }

    private suspend fun getMailsByCreationDate(startDate: OffsetDateTime, batchSize: Int): List<Mail> {
        return dao.findAllByMailStatusIdAndCreationDateAfter(
            NEW.id,
            startDate,
            Pageable.ofSize(batchSize).withPage(0)
        ).toList()
    }

    private suspend fun sendMails(mails: List<Mail>) {
        if (mails.isNotEmpty()) {
            val sent = emailSenderFacade.sendMimeMessages(mails)
            saveSendingResult(mails, sent)
        }
    }

    private suspend fun saveSendingResult(mails: List<Mail>, sent: Boolean) {
        log.info("Emails for mails $mails have been sent. Updating mail data.")
        // Here we can create retry mechanism instead of saving all batch with failed status.
        // Now it is for the sake of simplicity.
        val savedMails = dao.updateMails(
            mails.map { it.also { it.mailStatusId = if (sent) SENT.id else FAILED.id } }
        ).toList()
        savedMails.forEach { mail ->
            val statusDto = MailStatusCode.entries.first { it.id == mail.mailStatusId }
            kafkaSenderDecorator.sendMailBroadcast(
                event = MailBroadcast(id = mail.id, status = MailStatus.valueOf(statusDto.name)),
                metadata = MessageMetadata(attempt = 0, datetime = OffsetDateTime.now(), key = mail.id.toString()),
                topic = props.kafka.mailBroadcastTopic
            )
        }
    }
}
