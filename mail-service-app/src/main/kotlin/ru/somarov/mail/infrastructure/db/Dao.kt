package ru.somarov.mail.infrastructure.db

import kotlinx.coroutines.reactor.awaitSingle
import kotlinx.coroutines.reactor.awaitSingleOrNull
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.stereotype.Component
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import java.time.OffsetDateTime
import java.util.UUID

@Component
class Dao(
    private val mailRepo: MailRepo,
    private val template: ReactiveRedisTemplate<String, Mail>
) {
    // Cached operation
    suspend fun getMail(id: UUID): Mail {
        val ops = template.opsForValue()
        val cachedMail = ops.get("mails:$id").awaitSingleOrNull()
        val result = if (cachedMail == null) {
            val mail = mailRepo.findById(id) ?: throw IllegalArgumentException("Got id $id which doesn't exist")
            template.opsForSet().add("mails:$id", mail).awaitSingle()
            mail
        } else {
            cachedMail
        }
        return result
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
