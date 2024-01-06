package ru.somarov.mail.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MailService(private val repo: MailRepo) {
    private val log = LoggerFactory.getLogger(MailService::class.java)

    suspend fun createMail(email: String, text: String): Mail {
        log.info("Got register mail request with following text: $text, and mail: $email")
        return repo.save(createMailEntity(email, text))
    }

    suspend fun getMail(id: UUID): Mail {
        log.info("Got get mail request with following id: $id")
        return repo.findById(id) ?: throw IllegalArgumentException("Got id $id which doesn't exist")
    }

    private fun createMailEntity(email: String, text: String): Mail {
        return Mail(
            id = UUID.randomUUID(),
            clientEmail = email,
            text = text,
            mailStatusId = MailStatus.Companion.MailStatusCode.NEW.id,
            creationDate = OffsetDateTime.now(),
            lastUpdateDate = OffsetDateTime.now(),
            mailChannelId = MailChannel.Companion.MailChannelCode.MOBILE.id
        )
    }
}
