package ru.somarov.mail.infrastructure.service

import kotlinx.coroutines.flow.toList
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.NEW
import ru.somarov.mail.infrastructure.db.entity.MailStatus.Companion.MailStatusCode.SENT
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
            val sent = sendMimeMessages(mails)
            if (sent) {
                saveSendingResult(mails)
            }
        }
        return mails
    }

    private suspend fun saveSendingResult(mails: List<Mail>) {
        log.info("Emails for mails $mails have been sent. Updating mail data.")
        mails.forEach { mail ->
            mail.mailStatusId = SENT.id
            mail.lastUpdateDate = OffsetDateTime.now()
            mail.new = false
        }
        mailRepo.saveAll(mails).toList()
    }

    // Have to do TooGenericExceptionCaught to catch all types of network exceptions
    // Have to do SpreadOperator because it is java mail sender specification
    @Suppress("TooGenericExceptionCaught", "SpreadOperator")
    private fun sendMimeMessages(mails: List<Mail>): Boolean {
        mails.forEach { log.info("Sending message for mail $it") }

        var result = true

        try {
            emailSender.send(*(mails.map { it.createMimeMessage(props.contour.mail, emailSender) }.toTypedArray()))
        } catch (e: Exception) {
            log.error("Got exception while sending emails for mails $mails: $e")
            result = false
        }

        return result
    }
}
