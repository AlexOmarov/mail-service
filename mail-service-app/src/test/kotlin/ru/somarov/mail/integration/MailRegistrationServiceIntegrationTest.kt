package ru.somarov.mail.integration

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.somarov.mail.application.service.MailRegistrationService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.presentation.grpc.RegisterMailRequest
import java.util.UUID

class MailRegistrationServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var service: MailRegistrationService

    @Autowired
    lateinit var mailRepo: MailRepo

    @Test
    fun `When registerMail() add new Mail success`() = runBlocking {

        val text = "text"
        val email = "test@test.ru"

        val request = RegisterMailRequest.newBuilder()
            .setText(text)
            .setEmail(email)
            .build()

        val result = service.registerMail(request)

        val verify = mailRepo.findById(UUID.fromString(result.mail.id))

        assert(verify!!.text == text)
    }
}
