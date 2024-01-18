package ru.somarov.mail.infrastructure.db

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.reactive.awaitFirstOrNull
import kotlinx.coroutines.reactor.awaitSingle
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
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
    private val log = LoggerFactory.getLogger(this.javaClass)

    // Cached operation
    @Suppress("kotlin:S6518") // Cannot replace get with index accessor, redis throws an error
    suspend fun getMail(id: UUID): Mail {
        val ops = template.opsForSet()
        val cachedMail = ops.scan("mails:$id").awaitFirstOrNull()
        log.info("Got $cachedMail from redis")
        val result = if (cachedMail == null) {
            val mail = mailRepo.findById(id) ?: throw IllegalArgumentException("Got id $id which doesn't exist")
            ops.add("mails:$id", mail).awaitSingle()
            mail
        } else {
            cachedMail
        }
        return result
    }

    // Should put mail in cache here
    suspend fun createMail(email: String, text: String): Mail {
        return mailRepo.save(createMailEntity(email, text))
    }

    fun findAllByMailStatusIdAndCreationDateAfter(
        mailStatusId: UUID,
        creationDate: OffsetDateTime,
        page: Pageable
    ): Flow<Mail> {
        return mailRepo.findAllByMailStatusIdAndCreationDateAfter(mailStatusId, creationDate, page)
    }

    // Should refresh mail in cache here
    fun updateMails(mails: List<Mail>): Flow<Mail> {
        return mailRepo.saveAll(mails.map { it.also { it.lastUpdateDate = OffsetDateTime.now(); it.new = false } })
    }

    private fun createMailEntity(email: String, text: String): Mail {
        return Mail(
            uuid = UUID.randomUUID(),
            clientEmail = email,
            text = text,
            mailStatusId = MailStatus.Companion.MailStatusCode.NEW.id,
            creationDate = OffsetDateTime.now(),
            lastUpdateDate = OffsetDateTime.now(),
            mailChannelId = MailChannel.Companion.MailChannelCode.MOBILE.id
        )
    }
}
