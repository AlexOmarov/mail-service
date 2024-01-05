package ru.somarov.mail.tests.integration

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verifyBlocking
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import ru.somarov.mail.application.service.EmailService
import ru.somarov.mail.base.BaseIntegrationTest
import java.time.OffsetDateTime

private const val DELAY = 300L

@TestPropertySource(properties = ["contour.scheduling.email-sending.delay = $DELAY"])
private class EmailSendingSchedulerTests : BaseIntegrationTest() {
    @SpyBean
    lateinit var service: EmailService

    @Test
    fun `When scheduler starts it calls email service for sendNewEmails method`() {
        runBlocking { delay(DELAY + 100) }

        val captor = argumentCaptor<OffsetDateTime>()

        verifyBlocking(service, atLeastOnce()) {
            sendNewEmails(captor.capture())
        }

        val captured = captor.firstValue
        assert(
            captured.withSecond(0).withNano(0)
                .isEqual(OffsetDateTime.now().minusHours(24).withSecond(0).withNano(0))
        )
    }
}
