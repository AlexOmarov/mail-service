package ru.somarov.mail.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.db.entity.MailChannel
import ru.somarov.mail.infrastructure.db.entity.MailStatus
import ru.somarov.mail.infrastructure.db.repo.MailRepo
import ru.somarov.mail.presentation.grpc.MailDto
import ru.somarov.mail.presentation.grpc.RegisterMailRequest
import ru.somarov.mail.presentation.grpc.RegisterMailResponse
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MailRegistrationService(private val appealRepo: MailRepo) {
    private val log = LoggerFactory.getLogger(MailRegistrationService::class.java)
    suspend fun registerMail(request: RegisterMailRequest): RegisterMailResponse {
        log.info("Gor registerAppeal request with following text: ${request.text}, and mail: ${request.email}")

        val newMail = createMail(request)
        appealRepo.save(newMail)

        return RegisterMailResponse.newBuilder()
            .setMail(MailDto.newBuilder().setId(newMail.id.toString()))
            .build()
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
