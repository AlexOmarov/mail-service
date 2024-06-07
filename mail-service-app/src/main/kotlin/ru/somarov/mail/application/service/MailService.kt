package ru.somarov.mail.application.service

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.kafka.KafkaProducerFacade
import ru.somarov.mail.infrastructure.kafka.consumer.MessageMetadata
import ru.somarov.mail.presentation.dto.events.event.broadcast.MailBroadcast
import ru.somarov.mail.presentation.dto.events.event.broadcast.dto.MailStatus
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MailService(
    private val dao: Dao,
    private val props: ServiceProps,
    private val senderFacade: KafkaProducerFacade
) {
    private val log = LoggerFactory.getLogger(MailService::class.java)

    suspend fun createMail(email: String, text: String): Mail {
        log.info("Got register mail request with following text: $text, and mail: $email")
        val mail = dao.createMail(email, text)
        senderFacade.sendMailBroadcast(
            MailBroadcast(mail.id, MailStatus.NEW),
            MessageMetadata(OffsetDateTime.now(), mail.id.toString(), 0),
            props.kafka.mailBroadcastTopic
        )
        return mail
    }

    suspend fun getMail(id: UUID): Mail {
        log.info("Got get mail request with following id: $id")
        return dao.getMail(id)
    }
}
