package ru.somarov.mail.infrastructure.mail

import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.entity.Mail

@Component
class EmailSenderFacade(private val props: ServiceProps, private val emailSender: JavaMailSender) {
    private val log = LoggerFactory.getLogger(EmailSenderFacade::class.java)

    // Have to do TooGenericExceptionCaught to catch all types of network exceptions
    // Have to do SpreadOperator because it is java mail sender specification
    @Suppress("TooGenericExceptionCaught", "SpreadOperator")
    fun sendMimeMessages(mails: List<Mail>): Boolean {
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
