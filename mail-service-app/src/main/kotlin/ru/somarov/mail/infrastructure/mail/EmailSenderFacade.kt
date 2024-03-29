package ru.somarov.mail.infrastructure.mail

import org.slf4j.LoggerFactory
import org.springframework.mail.javamail.JavaMailSender
import org.springframework.stereotype.Component
import ru.somarov.mail.application.aggregate.MailAggregate
import ru.somarov.mail.infrastructure.config.ServiceProps

@Component
class EmailSenderFacade(
    private val emailSender: JavaMailSender,
    private val props: ServiceProps
) {
    private val log = LoggerFactory.getLogger(EmailSenderFacade::class.java)

    // Have to do TooGenericExceptionCaught to catch all types of network exceptions
    // Have to do SpreadOperator because it is java mail sender specification
    @Suppress("TooGenericExceptionCaught", "SpreadOperator")
    fun sendMimeMessages(mails: List<MailAggregate>): Boolean {
        mails.forEach { log.info("Sending message for mail $it") }

        var result = true

        try {
            val mimeMessages = mails.map {
                it.createMimeMessage(
                    systemUser = props.contour.mail.username,
                    destinationUser = props.contour.mail.destinationEmail,
                    mailSender = emailSender,
                    templatePath = "${TEMPLATE_FOLDER_PATH}/${props.contour.mail.template}"
                )
            }
            emailSender.send(*mimeMessages.toTypedArray())
        } catch (e: Exception) {
            log.error("Got exception while sending emails for mails $mails: $e")
            result = false
        }

        return result
    }

    companion object {
        const val TEMPLATE_FOLDER_PATH = "mail/templates"
    }
}
