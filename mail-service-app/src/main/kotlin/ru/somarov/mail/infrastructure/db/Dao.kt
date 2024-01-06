package ru.somarov.mail.infrastructure.db

import org.springframework.cache.annotation.Cacheable
import org.springframework.stereotype.Component
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import java.time.OffsetDateTime
import java.util.UUID

@Component
class Dao(private val mailRepo: MailRepo) {
    @Cacheable(value = ["mails"], key = "#id")
    suspend fun getMail(id: UUID): Mail {
        return mailRepo.findById(id) ?: throw IllegalArgumentException("Got id $id which doesn't exist")
    }

    suspend fun createMail(email: String, text: String): Mail {
        return mailRepo.save(createMailEntity(email, text))
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
