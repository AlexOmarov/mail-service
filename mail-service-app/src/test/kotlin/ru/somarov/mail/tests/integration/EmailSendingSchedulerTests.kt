package ru.somarov.mail.tests.integration

import org.awaitility.Awaitility
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.verifyBlocking
import org.springframework.boot.test.mock.mockito.SpyBean
import ru.somarov.mail.application.service.EmailService
import ru.somarov.mail.base.BaseIntegrationTest

private class EmailSendingSchedulerTests : BaseIntegrationTest() {
    @SpyBean
    lateinit var service: EmailService

    @Test
    fun `When scheduler starts it calls email service for sendNewEmails method`() {
        Awaitility.await().atMost(props.contour.scheduling.emailSending.delay).until {
            assertDoesNotThrow {
                verifyBlocking(service, atLeastOnce()) {
                    sendNewEmails(any())
                }
                return@assertDoesNotThrow true
            }
        }
    }
}
