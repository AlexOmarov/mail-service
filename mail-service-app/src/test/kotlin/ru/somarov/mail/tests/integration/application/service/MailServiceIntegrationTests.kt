package ru.somarov.mail.tests.integration.application.service

import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.reset
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.boot.test.mock.mockito.SpyBean
import org.springframework.data.redis.core.ReactiveRedisTemplate
import ru.somarov.mail.application.service.MailService
import ru.somarov.mail.base.BaseIntegrationTest
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import java.time.OffsetDateTime
import java.util.UUID

private class MailRegistrationServiceIntegrationTest : BaseIntegrationTest() {

    @Autowired
    lateinit var service: MailService

    @MockBean
    lateinit var mailRepo: MailRepo

    @SpyBean
    lateinit var redisTemplate: ReactiveRedisTemplate<String, Mail>

    @SpyBean
    lateinit var dao: Dao

    override fun beforeEach() {
        reset(dao, redisTemplate, mailRepo)
        runBlocking { doAnswer { it.arguments[0] as Mail }.whenever(mailRepo).save(any()) }
        runBlocking {
            doAnswer {
                Mail(
                    uuid = it.arguments[0] as UUID,
                    clientEmail = "email",
                    text = "text",
                    MailStatus.Companion.MailStatusCode.NEW.id,
                    MailChannel.Companion.MailChannelCode.MOBILE.id,
                    OffsetDateTime.now(),
                    OffsetDateTime.now()
                )
            }.whenever(mailRepo).findById(any())
        }
    }

    @Test
    fun `When registerMail method is called then mail is saved with valid text`() {

        val text = "text"
        val email = "test@test.ru"

        runBlocking { service.createMail(email, text) }

        val captor = argumentCaptor<Mail>()

        verifyBlocking(mailRepo, times(1)) {
            save(captor.capture())
        }
        assert(captor.firstValue.text == text)
        assert(captor.firstValue.clientEmail == email)
    }

    @Test
    fun `When get mail method then call cache`() {
        val mail = runBlocking { dao.createMail("email", "text") }

        // First call
        runBlocking { service.getMail(mail.id) }

        verifyBlocking(dao, times(1)) {
            getMail(eq(mail.id))
        }

        verifyBlocking(redisTemplate, times(2)) {
            opsForSet()
        }

        verifyBlocking(mailRepo, times(0)) {
            findById(mail.id)
        }

        // Second call
        runBlocking { service.getMail(mail.id) }

        verifyBlocking(dao, times(2)) {
            getMail(eq(mail.id))
        }

        verifyBlocking(redisTemplate, times(3)) {
            opsForSet()
        }

        verifyBlocking(mailRepo, times(0)) {
            findById(mail.id)
        }
    }
}
