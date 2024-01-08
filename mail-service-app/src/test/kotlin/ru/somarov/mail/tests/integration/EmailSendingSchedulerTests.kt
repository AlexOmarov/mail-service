package ru.somarov.mail.tests.integration

import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verifyBlocking
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.somarov.mail.application.service.EmailService
import ru.somarov.mail.base.BaseIntegrationTest
import java.time.OffsetDateTime

private class EmailSendingSchedulerTests : BaseIntegrationTest() {
    @SpyBean
    lateinit var service: EmailService

    @Test
    fun `When scheduler starts it calls email service for sendNewEmails method`() {
        val captor = argumentCaptor<OffsetDateTime>()
        Awaitility.await().atMost(props.contour.scheduling.emailSending.delay).until {
            assertDoesNotThrow {
                verifyBlocking(service, atLeastOnce()) {
                    sendNewEmails(captor.capture())
                }
                return@assertDoesNotThrow true
            }
        }

        val captured = captor.firstValue
        assert(
            captured.withSecond(0).withNano(0)
                .isEqual(OffsetDateTime.now().minusHours(24).withSecond(0).withNano(0))
        )
    }
}
