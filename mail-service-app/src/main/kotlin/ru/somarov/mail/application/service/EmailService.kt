package ru.somarov.mail.application.service

import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import ru.somarov.mail.application.aggregate.MailAggregate
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.FAILED
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.NEW
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.SENT
import ru.somarov.mail.infrastructure.kafka.KafkaProducerFacade
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.infrastructure.mail.EmailSenderFacade
import ru.somarov.mail.presentation.kafka.event.broadcast.MailBroadcast
import ru.somarov.mail.presentation.kafka.event.broadcast.dto.MailStatus
import java.time.OffsetDateTime

@Service
class EmailService(
    private val props: ServiceProps,
    private val dao: Dao,
    private val kafkaProducerFacade: KafkaProducerFacade,
    private val emailSenderFacade: EmailSenderFacade
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    suspend fun sendNewEmails(startDate: OffsetDateTime): Int {
        log.info("Started to get mails with unsent emails starting from $startDate")

        var iteration = 0
        var amountOfSentEmails = 0
        var mails: List<MailAggregate>?
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

    private suspend fun getMailsByCreationDate(startDate: OffsetDateTime, batchSize: Int): List<MailAggregate> {
        val mails = dao.findAllByMailStatusIdAndCreationDateAfter(
            NEW.id,
            startDate,
            Pageable.ofSize(batchSize).withPage(0)
        ).toList()
        return mails.map { mail -> MailAggregate(mail) }
    }

    private suspend fun sendMails(mails: List<MailAggregate>) {
        if (mails.isNotEmpty()) {
            val sent = emailSenderFacade.sendMimeMessages(mails)
            saveSendingResult(mails, sent)
        }
    }

    private suspend fun saveSendingResult(mails: List<MailAggregate>, sent: Boolean) {
        log.info("Emails for mails $mails have been sent. Updating mail data.")
        // Here we can create retry mechanism instead of saving all batch with failed status.
        // Now it is for the sake of simplicity.
        val savedMails = dao.updateMails(
            mails.map { it.mail }.map { it.also { it.mailStatusId = if (sent) SENT.id else FAILED.id } }
        ).toList()
        savedMails.forEach { mail ->
            val statusDto = MailStatusCode.entries.first { it.id == mail.mailStatusId }
            kafkaProducerFacade.sendMailBroadcast(
                event = MailBroadcast(id = mail.uuid, status = MailStatus.valueOf(statusDto.name)),
                metadata = MessageMetadata(attempt = 0, datetime = OffsetDateTime.now(), key = mail.uuid.toString()),
                topic = props.kafka.mailBroadcastTopic
            )
        }
    }
}
