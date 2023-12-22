package ru.somarov.mail.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verifyBlocking
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import ru.somarov.mail.application.service.EmailService
import ru.somarov.mail.base.BaseIntegrationTest
import java.time.OffsetDateTime

private const val DELAY = 300L

@TestPropertySource(properties = ["contour.scheduling.email-sending.delay = $DELAY"])
class EmailSendingSchedulerTests : BaseIntegrationTest() {
    @SpyBean
    lateinit var emailService: EmailService

    @Test
    fun `When scheduler starts it calls email service for sendEmailsForLatestMails method`() {
        runBlocking { delay(DELAY + 100) }
        verifyBlocking(emailService, atLeastOnce()) {
            sendNewEmails(OffsetDateTime.now().minusHours(24))
        }
    }
}
