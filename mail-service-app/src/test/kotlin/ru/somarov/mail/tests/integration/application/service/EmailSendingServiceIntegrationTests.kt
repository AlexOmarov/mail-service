package ru.somarov.mail.tests.integration.application.service

import jakarta.mail.internet.MimeMessage
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.springframework.beans.factory.annotation.Autowired
import ru.somarov.mail.application.service.EmailSenderService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import java.time.OffsetDateTime
import java.util.UUID

private class EmailSendingServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var service: EmailSenderService

    @Autowired
    lateinit var repository: MailRepo

    override fun beforeEach() {
        println("before each")
    }

    @Test
    fun `When emails are sent then email status changes to SENT`() {
        generateMails()

        val kk = runBlocking {
            service.sendNewEmails(OffsetDateTime.now().minusHours(24))
            repository.findAll().map { it.mailStatusId == MailStatus.Companion.MailStatusCode.SENT.id }.toList()
        }

        assert(kk.size == MAILS_AMOUNT)

        verifyBlocking(emailSenderImpl, times(1)) {
            // anyVararg doesn't work with kotlin for some reason, had to do it with captor
            // Probably relates to https://github.com/mockito/mockito-kotlin/issues/474
            send(*argumentCaptor<Array<MimeMessage>>().capture())
        }
    }

    private fun generateMails() = runBlocking {
        repository.saveAll(
            (1..MAILS_AMOUNT).map {
                Mail(
                    uuid = UUID.randomUUID(),
                    clientEmail = "qwerty$it@email.com",
                    text = "any text",
                    mailStatusId = MailStatus.Companion.MailStatusCode.NEW.id,
                    mailChannelId = MailChannel.Companion.MailChannelCode.MOBILE.id,
                    creationDate = OffsetDateTime.now(),
                    lastUpdateDate = OffsetDateTime.now(),
                )
            }
        ).toList()
    }

    companion object {
        const val MAILS_AMOUNT = 10
    }
}
