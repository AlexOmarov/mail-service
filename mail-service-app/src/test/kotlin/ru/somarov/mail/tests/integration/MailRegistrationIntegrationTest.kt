package ru.somarov.mail.tests.integration

import io.grpc.Metadata
import kotlinx.coroutines.runBlocking
import net.devh.boot.grpc.common.security.SecurityConstants
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.presentation.grpc.CreateMailRequest
import ru.somarov.mail.presentation.grpc.MailResponse
import ru.somarov.mail.util.DefaultEntitiesGenerator.createCreateMailRequest
import java.util.Base64

@TestPropertySource(properties = ["contour.scheduling.enabled = false"])
private class MailRegistrationIntegrationTest : BaseIntegrationTest() {
    @SpyBean
    lateinit var service: MailService

    @SpyBean
    lateinit var mailRepo: MailRepo

    override fun cleanAfterEach() {
        super.cleanAfterEach()
        reset(mailRepo)
    }

    @Test
    fun `When register mail request comes without required fields then treat like empty strings`() {
        val captor = argumentCaptor<Mail>()

        runBlocking {
            createMail(CreateMailRequest.newBuilder().build())
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
            createMail(
                createCreateMailRequest()
            )
        }
        verifyBlocking(service, times(1)) {
            createMail(any(), any())
        }
    }

    @Test
    fun `When mail service gets mail registration request it calls db to save mail`() {
        runBlocking {
            createMail(
                createCreateMailRequest()
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
            createMail(
                createCreateMailRequest(email = email)
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

    private suspend fun createMail(request: CreateMailRequest): MailResponse {
        val auth = props.contour.auth.user + ":" + props.contour.auth.password
        val encodedAuth = Base64.getEncoder().encode(auth.encodeToByteArray())
        val authHeader = "Basic " + String(encodedAuth)
        val metadata = Metadata().also { it.put(SecurityConstants.AUTHORIZATION_HEADER, authHeader) }
        return grpcClient.currentServiceClient.createMail(request, metadata)
    }
}
