package ru.somarov.mail.tests.integration.presentation.grpc

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.presentation.grpc.CreateMailRequest
import ru.somarov.mail.util.DefaultEntitiesGenerator.createCreateMailRequest

@TestPropertySource(properties = ["contour.scheduling.enabled = false"])
private class MailRegistrationIntegrationTest : BaseIntegrationTest() {
    @SpyBean
    lateinit var service: MailService

    @MockBean
    lateinit var mailRepo: MailRepo

    override fun beforeEach() {
        runBlocking {
            doAnswer { it.arguments[0] }.whenever(mailRepo).save(any())
        }
    }

    override fun cleanAfterEach() {
        super.cleanAfterEach()
        reset(mailRepo)
    }

    @Test
    fun `When register mail request comes without required fields then treat like empty strings`() {
        val captor = argumentCaptor<Mail>()

        runBlocking {
            grpcClient.createMail(
                CreateMailRequest.newBuilder().build(),
                props.contour.auth.user,
                props.contour.auth.password
            )
        }

        verifyBlocking(mailRepo, times(1)) {
            save(captor.capture())
        }
        val entityToSave = captor.firstValue
        assert(entityToSave.text == "")
        assert(entityToSave.clientEmail == "")
    }

    @Test
    fun `When register mail request comes then call service to process new mail`() {
        runBlocking {
            grpcClient.createMail(
                createCreateMailRequest(),
                props.contour.auth.user,
                props.contour.auth.password
            )
        }
        verifyBlocking(service, times(1)) {
            createMail(any(), any())
        }
    }

    @Test
    fun `When mail service gets mail registration request it calls db to save mail`() {
        runBlocking {
            grpcClient.createMail(
                createCreateMailRequest(),
                props.contour.auth.user,
                props.contour.auth.password
            )
        }
        verifyBlocking(mailRepo, times(1)) {
            save(any())
        }
    }

    @Test
    fun `When mail service gets mail registration request it fills saved mail with valid request params`() {
        val email = "test@gmail.com"
        val captor = argumentCaptor<Mail>()

        runBlocking {
            grpcClient.createMail(
                createCreateMailRequest(email = email),
                props.contour.auth.user,
                props.contour.auth.password
            )
        }
        verifyBlocking(mailRepo, times(1)) {
            save(captor.capture())
        }
        val entityToSave = captor.firstValue

        assert(entityToSave.mailStatusId == MailStatus.Companion.MailStatusCode.NEW.id)
        assert(entityToSave.new)
        assert(entityToSave.clientEmail == email)
    }
}
