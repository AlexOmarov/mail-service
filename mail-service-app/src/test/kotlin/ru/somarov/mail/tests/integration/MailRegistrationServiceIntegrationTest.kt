package ru.somarov.mail.tests.integration

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.presentation.grpc.RegisterMailRequest

private class MailRegistrationServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var service: MailService

    @Autowired
    lateinit var mailRepo: MailRepo

    @Test
    fun `When registerMail method is called then mail is saved with valid text`() {

        val text = "text"
        val email = "test@test.ru"

        val request = RegisterMailRequest.newBuilder()
            .setText(text)
            .setEmail(email)
            .build()

        val mail = runBlocking {
            val result = service.registerMail(request)
            mailRepo.findById(result.id)!!
        }

        assert(mail.text == text)
    }
}
