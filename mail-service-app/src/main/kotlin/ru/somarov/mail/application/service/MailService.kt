package ru.somarov.mail.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.presentation.grpc.RegisterMailRequest
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MailService(private val repo: MailRepo) {
    private val log = LoggerFactory.getLogger(MailService::class.java)

    suspend fun registerMail(request: RegisterMailRequest): Mail {
        log.info("Got register mail request with following text: ${request.text}, and mail: ${request.email}")
        return repo.save(createMail(request))
    }

    suspend fun getMail(id: UUID): Mail {
        log.info("Got get mail request with following id: $id")
        return repo.findById(id) ?: throw IllegalArgumentException("Got id $id which doesn't exist")
    }

    private fun createMail(request: RegisterMailRequest): Mail {
        return Mail(
            id = UUID.randomUUID(),
            clientEmail = request.email,
            text = request.text,
            mailStatusId = MailStatus.Companion.MailStatusCode.NEW.id,
            creationDate = OffsetDateTime.now(),
            lastUpdateDate = OffsetDateTime.now(),
            mailChannelId = MailChannel.Companion.MailChannelCode.MOBILE.id
        )
    }
}
