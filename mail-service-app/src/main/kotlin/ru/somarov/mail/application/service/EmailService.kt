package ru.somarov.mail.application.service

import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.FAILED
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.NEW
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.SENT
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.infrastructure.kafka.KafkaSenderDecorator
import ru.somarov.mail.infrastructure.kafka.MessageMetadata
import ru.somarov.mail.infrastructure.mail.EmailSenderFacade
import ru.somarov.mail.presentation.kafka.event.broadcast.MailBroadcast
import ru.somarov.mail.presentation.kafka.event.broadcast.dto.MailStatus
import java.time.OffsetDateTime

@Service
class EmailService(
    private val props: ServiceProps,
    private val mailRepo: MailRepo,
    private val kafkaSenderDecorator: KafkaSenderDecorator,
    private val emailSenderFacade: EmailSenderFacade
) {
    private val log = LoggerFactory.getLogger(EmailService::class.java)
    suspend fun sendNewEmails(startDate: OffsetDateTime): Int {
        log.info("Started to get mails with unsent emails starting from $startDate")

        var iteration = 0
        var amountOfSentEmails = 0
        var mails: List<Mail>?

        do {
            log.info("Mail sending, iteration $iteration")

            mails = processIteration(startDate)
            amountOfSentEmails += mails.size

            iteration++
        } while (!mails.isNullOrEmpty())

        log.info("Finished sending emails for unprocessed mails from $startDate")
        return amountOfSentEmails
    }

    private suspend fun processIteration(startDate: OffsetDateTime): List<Mail> {
        val batchSize = props.contour.scheduling.emailSending.batchSize

        val mails = mailRepo.findAllByMailStatusIdAndCreationDateAfter(
            NEW.id,
            startDate,
            Pageable.ofSize(batchSize).withPage(0)
        ).toList()

        if (mails.isNotEmpty()) {
            val sent = emailSenderFacade.sendMimeMessages(mails)
            if (sent) {
                saveSendingResult(mails)
            } else {
                // Here we can create retry mechanism instead of saving all batch in failed status.
                // Now it is for the sake of simplicity
                mailRepo.saveAll(
                    mails.map { mail ->
                        mail.mailStatusId = FAILED.id
                        mail.lastUpdateDate = OffsetDateTime.now()
                        mail.new = false
                        mail
                    }
                ).toList()

            }
        }
        return mails
    }

    private suspend fun saveSendingResult(mails: List<Mail>) {
        log.info("Emails for mails $mails have been sent. Updating mail data.")
        val savedMails = mailRepo.saveAll(
            mails.map { mail ->
                mail.mailStatusId = SENT.id
                mail.lastUpdateDate = OffsetDateTime.now()
                mail.new = false
                mail
            }
        ).toList()
        savedMails.forEach { mail ->
            val statusDto =
                ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.entries
                    .first { it.id == mail.mailStatusId }
            kafkaSenderDecorator.sendMailBroadcast(
                MailBroadcast(mail.id, MailStatus.valueOf(statusDto.name)),
                MessageMetadata(attempt = 0, datetime = OffsetDateTime.now(), key = mail.id.toString()),
                props.kafka.mailBroadcastTopic
            )
        }
    }
}
