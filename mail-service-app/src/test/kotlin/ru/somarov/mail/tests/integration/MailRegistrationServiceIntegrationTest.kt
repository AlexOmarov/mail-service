package ru.somarov.mail.tests.integration

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.presentation.grpc.RegisterMailRequest
import java.util.UUID

private class MailRegistrationServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var service: MailService

    @Autowired
    lateinit var mailRepo: MailRepo

    @Test
    fun `When registerMail() add new Mail success`() {

        val text = "text"
        val email = "test@test.ru"

        val request = RegisterMailRequest.newBuilder()
            .setText(text)
            .setEmail(email)
            .build()

        val mail = runBlocking {
            val result = service.registerMail(request)
            mailRepo.findById(UUID.fromString(result.mail.id))!!
        }

        assert(mail.text == text)
    }
}
