package ru.somarov.mail.integration

import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.reduce
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

class EmailSendingServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var emailService: EmailService

    @Autowired
    lateinit var repository: MailRepo

    @Test
    fun `When email service gets send email request then`() = runBlocking {
        generateMails()

        emailService.sendLatestEmails()

        assert(repository.findAll().map { it.mailStatusId == MailStatus.Companion.MailStatusCode.SENT.id }
            .reduce { accumulator, value -> accumulator && value })
    }

    private fun generateMails(elements: Int = 10) = runBlocking {
        repository.saveAll(
            (1..elements).map {
                Mail(
                    id = UUID.randomUUID(),
                    clientEmail = "qwerty$it@email.com",
                    text = "any text",
                    mailStatusId = MailStatus.Companion.MailStatusCode.NEW.id,
                    mailChannelId = MailChannel.Companion.MailChannelCode.MOBILE.id,
                    creationDate = OffsetDateTime.now(),
                    lastUpdateDate = OffsetDateTime.now(),
                )
            }
        )
    }
}
