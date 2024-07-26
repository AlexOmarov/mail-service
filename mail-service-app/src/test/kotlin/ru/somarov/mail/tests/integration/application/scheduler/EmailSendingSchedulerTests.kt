package ru.somarov.mail.tests.integration.application.scheduler

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.timeout
import org.mockito.kotlin.verifyBlocking
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import ru.somarov.mail.application.service.EmailSenderService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.infrastructure.mail.EmailSenderFacade
import java.time.OffsetDateTime
import java.util.UUID

@TestPropertySource(
    properties = [
        "contour.scheduling.enabled = true",
        "contour.scheduling.email-sending.enabled = true",
    ]
)
class EmailSendingSchedulerTests : BaseIntegrationTest() {
    @SpyBean
    lateinit var service: EmailSenderService

    @SpyBean
    lateinit var facade: EmailSenderFacade

    @Autowired
    lateinit var mailRepo: MailRepo

    override fun beforeEach() {
        println("before each")
    }

    @Test
    fun `When scheduler starts it calls email service for sendNewEmails method`() {
        runBlocking {
            mailRepo.saveAll(
                listOf(
                    Mail(
                        UUID.randomUUID(),
                        clientEmail = "sfdg",
                        text = "sfg",
                        MailStatus.Companion.MailStatusCode.NEW.id,
                        MailChannel.Companion.MailChannelCode.MOBILE.id,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                    ),
                    Mail(
                        UUID.randomUUID(),
                        clientEmail = "sfdg",
                        text = "sfg",
                        MailStatus.Companion.MailStatusCode.NEW.id,
                        MailChannel.Companion.MailChannelCode.MOBILE.id,
                        OffsetDateTime.now(),
                        OffsetDateTime.now()
                    )
                )
            ).toList()
        }
        val delay = props.contour.scheduling.emailSending.delay

        verifyBlocking(service, timeout((delay + delay).toMillis()).atLeastOnce()) {
            sendNewEmails(any())
        }
        verifyBlocking(facade, timeout((delay).toMillis()).atLeastOnce()) {
            sendMimeMessages(any())
        }
    }
}
