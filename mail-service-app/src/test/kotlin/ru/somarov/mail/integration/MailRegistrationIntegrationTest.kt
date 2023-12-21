package ru.somarov.mail.integration

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.test.context.TestPropertySource
import ru.somarov.mail.application.service.MailRegistrationService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.presentation.grpc.RegisterMailRequest
import ru.somarov.mail.util.DefaultEntitiesGenerator.createRegisterMailRequest

@TestPropertySource(properties = ["contour.scheduling.enabled = false"])
class MailRegistrationIntegrationTest : BaseIntegrationTest() {
    @SpyBean
    lateinit var service: MailRegistrationService

    @SpyBean
    lateinit var mailRepo: MailRepo

    override fun cleanAfterEach() {
        super.cleanAfterEach()
        reset(mailRepo)
    }

    @Test
    fun `When register mail request comes without required fields then treat like empty strings`() {
        val captor = argumentCaptor<Mail>()
        val tokenCaptor = argumentCaptor<String>()

        runBlocking {
            grpcTestClient.registerMail(RegisterMailRequest.newBuilder().build())
        }

        val tokenPassedToMonolith = tokenCaptor.firstValue
        assert(tokenPassedToMonolith == "")

        verifyBlocking(mailRepo, times(1)) {
            save(captor.capture())
        }
        val entityToSave = captor.firstValue
        assert(entityToSave.text == "")
    }

    @Test
    fun `When register mail request comes then call service to process new mail`() {
        runBlocking {
            grpcTestClient.registerMail(
                createRegisterMailRequest()
            )
        }
        verifyBlocking(service, times(1)) {
            registerMail(any())
        }
    }

    @Test
    fun `When mail service gets mail registration request it calls db to save mail`() {
        runBlocking {
            grpcTestClient.registerMail(
                createRegisterMailRequest()
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
            grpcTestClient.registerMail(
                createRegisterMailRequest()
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
