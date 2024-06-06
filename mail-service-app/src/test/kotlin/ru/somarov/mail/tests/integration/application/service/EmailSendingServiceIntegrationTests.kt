package ru.somarov.mail.tests.integration.application.service

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.somarov.mail.application.service.EmailSenderService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.infrastructure.mail.EmailSenderFacade
import java.time.OffsetDateTime
import java.util.UUID

private class EmailSendingServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var service: EmailSenderService

    @SpyBean
    lateinit var facade: EmailSenderFacade

    @Autowired
    lateinit var repository: MailRepo

    override fun beforeEach() {
        println("before each")
        doReturn(true).whenever(facade).sendMimeMessages(any())
    }

    @Test
    fun `When emails are sent then email status changes to SENT`() = runBlocking {
        repository.saveAll(generateMails()).toList()
        generateMails()

        val kk = runBlocking {
            service.sendNewEmails(OffsetDateTime.now().minusHours(24))
            repository.findAll().map { it.mailStatusId == MailStatus.Companion.MailStatusCode.SENT.id }.toList()
        }

        assert(kk.size == MAILS_AMOUNT)
    }

    private fun generateMails() =
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

    companion object {
        const val MAILS_AMOUNT = 10
    }
}
