package ru.somarov.mail.tests.integration

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.somarov.mail.application.service.EmailService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import java.time.OffsetDateTime
import java.util.UUID

private class EmailSendingServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var service: EmailService

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
