package ru.somarov.mail.application.service

import com.fasterxml.jackson.databind.ObjectMapper
import io.micrometer.observation.ObservationRegistry
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import ru.somarov.mail.infrastructure.config.ServiceProps
import ru.somarov.mail.infrastructure.db.Dao
import ru.somarov.mail.infrastructure.db.entity.Mail
import ru.somarov.mail.infrastructure.kafka.Metadata
import ru.somarov.mail.infrastructure.kafka.Producer
import ru.somarov.mail.infrastructure.kafka.Producer.ProducerProps
import ru.somarov.mail.presentation.dto.event.broadcast.MailBroadcast
import java.time.OffsetDateTime
import java.util.UUID

@Service
class MailService(
    private val dao: Dao,
    props: ServiceProps,
    mapper: ObjectMapper,
    registry: ObservationRegistry
) {
    private val log = LoggerFactory.getLogger(MailService::class.java)

    private val producer = Producer<MailBroadcast>(
        mapper,
        ProducerProps(props.kafka.brokers, props.kafka.sender.maxInFlight, props.kafka.mailBroadcastTopic), registry
    )

    suspend fun createMail(email: String, text: String): Mail {
        log.info("Got register mail request with following text: $text, and mail: $email")
        val mail = dao.createMail(email, text)
        producer.send(
            MailBroadcast(mail.id, MailBroadcast.MailStatus.NEW),
            Metadata(OffsetDateTime.now(), mail.id.toString(), 0)
        )
        return mail
    }

    suspend fun getMail(id: UUID): Mail {
        log.info("Got get mail request with following id: $id")
        return dao.getMail(id)
    }
}
